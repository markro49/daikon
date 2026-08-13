package daikon.dcomp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import daikon.chicory.ClassInfo;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.junit.Test;

/**
 * Unit tests for {@link DCInstrument24}.
 *
 * <p>Note that this test must be located in daikon.dcomp rather than daikon.test.dcomp as it needs
 * to access protected fields of DCRuntime and Premain.
 */
public final class DCInstrumentTest24 {

  /** Creates a new DCInstrumentTest24. */
  public DCInstrumentTest24() {}

  /** A small class that the tests instrument. */
  public static class Sample {

    /** An arbitrary value. */
    int value;

    /** Creates a new Sample. */
    public Sample() {
      value = 0;
    }

    /**
     * Adds to {@link #value}.
     *
     * @param x the amount to add
     * @return the new value
     */
    public int add(int x) {
      value += x;
      return value;
    }
  }

  /**
   * Tests that a class instrumented by {@link DCInstrument24#instrument_jdk_class} calls the shadow
   * runtime class {@code java.lang.DCRuntime} rather than {@code daikon.dcomp.DCRuntime}. A class
   * in a pre-instrumented {@code java.base} module may not refer to anything outside {@code
   * java.base}. This must hold even when the DCInstrument24 constructor chose {@code
   * daikon.dcomp.DCRuntime}, which it does whenever {@code Premain.jdk_instrumented} is false.
   *
   * @throws IOException if the class file for {@link Sample} cannot be read
   */
  @Test
  public void testJdkClassCallsShadowRuntime() throws IOException {
    boolean savedJdkInstrumented = Premain.jdk_instrumented;
    @BinaryName String savedInstrumentationInterface = DCRuntime.instrumentation_interface;
    Premain.jdk_instrumented = false;
    // BuildJDK24 sets this static field before each class it instruments.
    DCRuntime.instrumentation_interface = "daikon.dcomp.DCompInstrumented";
    byte[] instrumented;
    try {
      instrumented = instrumentAsJdkClass();
    } finally {
      Premain.jdk_instrumented = savedJdkInstrumented;
      DCRuntime.instrumentation_interface = savedInstrumentationInterface;
    }
    Set<String> invoked = invokedClasses(instrumented);
    assertTrue(
        "instrumented class does not call java/lang/DCRuntime: " + invoked,
        invoked.contains("java/lang/DCRuntime"));
    assertFalse(
        "instrumented class calls daikon/dcomp/DCRuntime: " + invoked,
        invoked.contains("daikon/dcomp/DCRuntime"));
  }

  /**
   * Instruments {@link Sample} as if it were a JDK class.
   *
   * @return the instrumented bytes of {@link Sample}
   * @throws IOException if the class file for {@link Sample} cannot be read
   */
  private byte[] instrumentAsJdkClass() throws IOException {
    @SuppressWarnings("signature:assignment") // the name of a nested class
    @BinaryName String classname = Sample.class.getName();
    InputStream sampleStream =
        DCInstrumentTest24.class.getResourceAsStream("DCInstrumentTest24$Sample.class");
    if (sampleStream == null) {
      throw new Error("cannot find the class file for " + classname);
    }
    byte[] original;
    try (InputStream is = sampleStream) {
      original = is.readAllBytes();
    }
    ClassFile classFile = ClassFile.of();
    ClassModel classModel = classFile.parse(original);
    ClassInfo classInfo = new ClassInfo(classname, DCInstrumentTest24.class.getClassLoader());
    DCInstrument24 dci = new DCInstrument24(classFile, classModel, true);
    byte[] instrumented = dci.instrument_jdk_class(classInfo);
    if (instrumented == null) {
      throw new Error("instrumentation of " + classname + " failed");
    }
    return instrumented;
  }

  /**
   * Returns the names, in internal form, of the classes that the methods of the given class file
   * invoke a method of. Unlike the constant pool, this contains only classes that the code actually
   * calls.
   *
   * @param classBytes the bytes of a class file
   * @return the internal names of the classes whose methods {@code classBytes} invokes
   */
  private Set<String> invokedClasses(byte[] classBytes) {
    Set<String> result = new HashSet<>();
    ClassModel classModel = ClassFile.of().parse(classBytes);
    for (MethodModel method : classModel.methods()) {
      method.code().ifPresent(code -> addInvokedClasses(code, result));
    }
    return result;
  }

  /**
   * Adds, to {@code result}, the internal name of the owner of each invocation instruction in the
   * given method body.
   *
   * @param code the body of a method
   * @param result the set to add to; is side-effected by this method
   */
  private void addInvokedClasses(CodeModel code, Set<String> result) {
    for (CodeElement element : code) {
      if (element instanceof InvokeInstruction invoke) {
        result.add(invoke.owner().asInternalName());
      }
    }
  }
}
