package proto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;

/**
 * Prototype of the call-site transform: wrap the argument of RoomDatabase.Builder.setDriver(...)
 * with io.sentry.sqlite.SentrySQLiteDriver.create(...), skipping the SupportSQLiteDriver bridge and
 * already-wrapped / erased-interface arguments.
 *
 * Reads classes/demo/Caller.class, rewrites it in place. Uses COMPUTE_MAXS only (no COMPUTE_FRAMES,
 * so no classes are loaded by the writer): inserting create() has net-zero stack effect (pops one
 * SQLiteDriver, pushes one SQLiteDriver), so existing stack-map frames stay valid.
 */
public final class DriverWrapTransformer {

  static final int API = Opcodes.ASM9;
  static final String DRIVER_IFACE = "androidx/sqlite/SQLiteDriver";
  static final String BRIDGE = "androidx/sqlite/driver/SupportSQLiteDriver";
  static final String SENTRY = "io/sentry/sqlite/SentrySQLiteDriver";

  public static void main(String[] args) throws Exception {
    Path classFile = Paths.get(args[0]); // e.g. classes/demo/Caller.class
    byte[] original = Files.readAllBytes(classFile);

    ClassReader reader = new ClassReader(original);
    // COMPUTE_MAXS recomputes max stack/locals; frames are preserved as-is (valid, see above).
    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
    reader.accept(new TransformClassVisitor(writer), 0);

    Files.write(classFile, writer.toByteArray());
    System.out.println("[transformer] rewrote " + classFile);
  }

  static final class TransformClassVisitor extends ClassVisitor {
    private String owner;

    TransformClassVisitor(ClassVisitor cv) {
      super(API, cv);
    }

    @Override
    public void visit(int v, int a, String name, String sig, String sup, String[] ifaces) {
      this.owner = name;
      super.visit(v, a, name, sig, sup, ifaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
      MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
      if (mv == null) return null;
      // AnalyzerAdapter must sit "above" our logic so this.stack is current when we inspect it.
      WrapMethodVisitor wrap = new WrapMethodVisitor(owner, access, name, desc, mv);
      return wrap;
    }
  }

  /**
   * AnalyzerAdapter tracks the operand-stack types. Before delegating a matching setDriver call, the
   * top of stack is the driver argument; we read its static type to decide wrap vs skip.
   */
  static final class WrapMethodVisitor extends AnalyzerAdapter {
    WrapMethodVisitor(String owner, int access, String name, String desc, MethodVisitor mv) {
      super(API, owner, access, name, desc, mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      // owner-agnostic match: name + first arg is SQLiteDriver (covers androidx.room & androidx.room3)
      boolean isSetDriver = name.equals("setDriver") && desc.startsWith("(L" + DRIVER_IFACE + ";)");
      if (isSetDriver && this.stack != null && !this.stack.isEmpty()) {
        Object top = this.stack.get(this.stack.size() - 1);
        String topType = (top instanceof String) ? (String) top : String.valueOf(top);
        boolean wrap = isWrappable(topType);
        System.out.println(
            "  [decision] setDriver arg static type = " + topType + "  ->  " + (wrap ? "WRAP" : "SKIP"));
        if (wrap) {
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC, SENTRY, "create", "(L" + DRIVER_IFACE + ";)L" + DRIVER_IFACE + ";", false);
        }
      }
      super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private static boolean isWrappable(String t) {
      if (t == null) return false;
      if (t.equals(BRIDGE)) return false; // never wrap the bridge -> the no-double-wrap guarantee
      if (t.equals(SENTRY)) return false; // already a Sentry driver
      if (t.equals(DRIVER_IFACE)) return false; // erased to bare interface -> bias to false-negative
      return true; // any other concrete type assignable to SQLiteDriver
    }
  }
}
