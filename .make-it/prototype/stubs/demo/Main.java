package demo;

/** Runs every scenario on the (transformed) Caller. Loading + running = JVM verifier pass. */
public class Main {
  public static void main(String[] args) {
    Caller c = new Caller();
    System.out.println("A) inline real driver  (expect WRAP -> SentrySQLiteDriver):");
    c.scenarioA_inlineRealDriver();
    System.out.println("B) inline bridge        (expect SKIP -> SupportSQLiteDriver):");
    c.scenarioB_inlineBridge();
    System.out.println("C) concrete local       (expect WRAP -> SentrySQLiteDriver):");
    c.scenarioC_localRealDriver();
    System.out.println("D) erased local bridge  (expect SKIP -> SupportSQLiteDriver):");
    c.scenarioD_erasedLocalBridge();
    System.out.println("E) manual wrap          (expect SKIP -> SentrySQLiteDriver, not doubled):");
    c.scenarioE_manualWrap();
    System.out.println("F) factory return       (expect SKIP -> AndroidSQLiteDriver, false-neg):");
    c.scenarioF_factoryReturn();
    System.out.println("DONE (transformed Caller loaded & ran => JVM verifier accepted it)");
  }
}
