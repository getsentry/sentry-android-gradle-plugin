package proto;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

/** Prints a textual disassembly of a .class file (to eyeball injected create() calls). */
public final class Disassemble {
  public static void main(String[] args) throws Exception {
    byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
    ClassReader reader = new ClassReader(bytes);
    reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
  }
}
