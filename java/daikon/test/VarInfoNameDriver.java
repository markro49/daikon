package daikon.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import daikon.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This is called by VarInfoName to parse varInfoNameTest<em>foo</em> files and then apply various
 * transformation tests on them. To add your own test, follow directions in VarInfoNameTest.
 */
@SuppressWarnings("nullness") // testing code
public class VarInfoNameDriver {

  /**
   * Convenience entry point for TraceSelect.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    daikon.LogHelper.setupLogs(INFO);
    run(System.in, System.out);
  }

  private static final Map<String, Handler> handlers = new HashMap<>();

  public static void run(InputStream commands, PrintStream output) {
    try {
      run_helper(commands, output);
    } catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
  }

  private static void run_helper(InputStream _commands, PrintStream output) throws IOException {
    BufferedReader commands = new BufferedReader(new InputStreamReader(_commands, UTF_8));
    Map<String, VarInfoName> variables = new HashMap<>();

    String command;
    while ((command = commands.readLine()) != null) {
      if (command.startsWith(";")) {
        output.println(command);
        continue;
      }

      // tokenize arguments
      StringTokenizer tok = new StringTokenizer(command);
      Deque<String> list = new ArrayDeque<String>();
      while (tok.hasMoreTokens()) {
        list.add(tok.nextToken());
      }

      // ignore blank lines
      if (list.size() == 0) {
        continue;
      }

      output.println("; " + command);

      // call the handler
      String method = list.removeFirst();
      String[] args = list.toArray(new String[0]);
      Handler handler = handlers.get(method);
      if (handler == null) {
        throw new UnsupportedOperationException("Unknown method: " + method);
      }
      handler.handle(variables, args, output);
    }
  }

  public static interface Handler {
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out);
  }

  // VarInfoName parse(String);
  private static class Parse implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      String var = args[0];
      String expr = args[1];
      VarInfoName parse = VarInfoName.parse(expr);
      vars.put(var, parse);
      out.println(var + " = " + parse.name());
    }
  }

  static {
    handlers.put("parse", new Parse());
  }

  // String name();
  private static class Name implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(1, args.length);
      VarInfoName var = vars.get(args[0]);
      out.println(var.name());
    }
  }

  static {
    handlers.put("name", new Name());
  }

  // String esc_name();
  private static class EscName implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(1, args.length);
      VarInfoName var = vars.get(args[0]);
      out.println(var.esc_name());
    }
  }

  static {
    handlers.put("esc_name", new EscName());
  }

  // String simplify_name();
  private static class SimplifyName implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(1, args.length);
      VarInfoName var = vars.get(args[0]);
      out.println(var.simplify_name());
    }
  }

  static {
    handlers.put("simplify_name", new SimplifyName());
  }

  // String[] QuantHelper.format_esc(VarInfoName[] roots)
  private static class QuantifyEscName implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      VarInfoName[] roots = new VarInfoName[args.length];
      for (int i = 0; i < args.length; i++) {
        roots[i] = vars.get(args[i]);
      }
      String[] result = VarInfoName.QuantHelper.format_esc(roots);
      String result2 = VarInfoName.QuantHelper.format_esc(roots, true)[0];
      for (int i = 0; i < result.length; i++) {
        if (i != 0 && i != result.length - 1) {
          out.print('\t');
        }
        out.println(result[i]);
        if (i == 0 && roots.length > 1) {
          out.println(result2);
        }
      }
    }
  }

  static {
    handlers.put("quantify_esc_name", new QuantifyEscName());
  }

  // String[] QuantHelper.format_simplify(VarInfoName[] roots)
  private static class QuantifySimplifyName implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      VarInfoName[] roots = new VarInfoName[args.length];
      for (int i = 0; i < args.length; i++) {
        roots[i] = vars.get(args[i]);
      }
      String[] result = VarInfoName.QuantHelper.format_simplify(roots);
      for (int i = 0; i < result.length; i++) {
        if (i != 0 && i != result.length - 1) {
          out.print('\t');
        }
        out.println(result[i]);
      }
    }
  }

  static {
    handlers.put("quantify_simplify_name", new QuantifySimplifyName());
  }

  // VarInfoName intern();
  // TODO

  // boolean isLiteralConstant();
  // TODO

  // boolean equals(Object);
  // boolean equals(VarInfoName);
  private static class Equals implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      VarInfoName a = vars.get(args[0]);
      VarInfoName b = vars.get(args[1]);
      out.println(args[0] + " " + (a.equals(b) ? "=" : "!") + "= " + args[1]);
    }
  }

  static {
    handlers.put("equals", new Equals());
  }

  // int hashCode(@GuardSatisfied VarInfoNameDriver this);
  private static class HashCode implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      VarInfoName a = vars.get(args[0]);
      VarInfoName b = vars.get(args[1]);
      out.println(
          args[0]
              + ".hash "
              + ((a.hashCode() == b.hashCode()) ? "=" : "!")
              + "= "
              + args[1]
              + ".hash");
    }
  }

  static {
    handlers.put("hash", new HashCode());
  }

  // String toString(@GuardSatisfied VarInfoNameDriver this);

  // VarInfoName applySize();
  private static class Size implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      VarInfoName var = vars.get(args[1]);
      VarInfoName result = var.applySize();
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("size", new Size());
  }

  // VarInfoName applyFunction(String);
  private static class Function implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(3, args.length);
      String func = args[1];
      VarInfoName var = vars.get(args[2]);
      VarInfoName result = var.applyFunction(func);
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("function", new Function());
  }

  // VarInfoName applyTypeOf();
  private static class TypeOf implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      VarInfoName var = vars.get(args[1]);
      VarInfoName result = var.applyTypeOf();
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("typeof", new TypeOf());
  }

  // VarInfoName applyPrestate();
  private static class Prestate implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      VarInfoName var = vars.get(args[1]);
      VarInfoName result = var.applyPrestate();
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("prestate", new Prestate());
  }

  // VarInfoName applyPoststate();
  private static class Poststate implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      VarInfoName var = vars.get(args[1]);
      VarInfoName result = var.applyPoststate();
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("poststate", new Poststate());
  }

  // VarInfoName.PostPreConverter visitor test
  private static class PostPreConverter implements Handler {
    private static VarInfoName.PostPreConverter converter = new VarInfoName.PostPreConverter();

    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      VarInfoName var = vars.get(args[0]);
      VarInfoName result = converter.replace(var);
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("postPreConverter", new PostPreConverter());
  }

  // VarInfoName applyAdd(int);
  private static class Add implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(3, args.length);
      VarInfoName var = vars.get(args[1]);
      int amt = Integer.parseInt(args[2]);
      VarInfoName result = var.applyAdd(amt);
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("add", new Add());
  }

  // VarInfoName applyElements();
  private static class Elements implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(2, args.length);
      VarInfoName var = vars.get(args[1]);
      VarInfoName result = var.applyElements();
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("elements", new Elements());
  }

  // VarInfoName applySubscript(VarInfoName);
  private static class Subscript implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(3, args.length);
      VarInfoName var = vars.get(args[1]);
      VarInfoName sub = vars.get(args[2]);
      VarInfoName result = var.applySubscript(sub);
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("subscript", new Subscript());
  }

  // VarInfoName applySlice(VarInfoName, VarInfoName);
  private static class Slice implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(4, args.length);
      VarInfoName var = vars.get(args[1]);
      VarInfoName i = vars.get(args[2]);
      VarInfoName j = vars.get(args[3]);
      VarInfoName result = var.applySlice(i, j);
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("slice", new Slice());
  }

  // String jml_name()
  private static class JMLName implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(1, args.length);
      VarInfoName var = vars.get(args[0]);
      VarInfoName.testCall = true;
      out.println(args[0] + ".jml_name() = " + var.jml_name(null));
      VarInfoName.testCall = false;
    }
  }

  static {
    handlers.put("jml_name", new JMLName());
  }

  // VarInfoName applyFunctionOfN(String function, List vars)
  private static class FunctionOfN implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertTrue(args.length >= 4);
      String func = args[1];
      List<VarInfoName> function_vars = new ArrayList<>();
      for (int x = 2; x < args.length; x++) {
        function_vars.add(vars.get(args[x]));
      }
      VarInfoName result = VarInfoName.applyFunctionOfN(func, function_vars);
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("function_of_N", new FunctionOfN());
  }

  // public VarInfoName applyField(String field)
  private static class Field implements Handler {
    @Override
    public void handle(Map<String, VarInfoName> vars, String[] args, PrintStream out) {
      assertEquals(3, args.length);
      VarInfoName var = vars.get(args[1]);
      String fieldname = args[2];
      VarInfoName result = var.applyField(fieldname);
      vars.put(args[0], result);
      out.println(args[0] + " = " + result.name());
    }
  }

  static {
    handlers.put("field", new Field());
  }

  // public String[] QuantHelper.format_jml(VarInfoName[] roots)
  //   private static class QuantifyFormatJML implements Handler {
  //     public void handle(Map<String,VarInfoName> vars, String[] args, PrintStream out) {
  //       assertTrue (args.length >= 1);
  //       VarInfoName roots[] = new VarInfoName [args.length];
  //       for (int x = 0; x<args.length; x++)
  //         roots[x] = vars.get(args[x]);
  //       String result[] = VarInfoName.QuantHelper.format_jml(roots);
  //       for (int x = 0; x<result.length; x++)
  //         out.println(result[x]);
  //     }
  //   }
  //   static { handlers.put("quantify_format_jml", new QuantifyFormatJML()); }

  //   // public String[] QuantHelper.format_jml(VarInfoName[] roots, boolean elementwise)
  //   private static class QuantifyFormatJMLElementwise implements Handler {
  //     public void handle(Map<String,VarInfoName> vars, String[] args, PrintStream out) {
  //       assertTrue (args.length >= 1);
  //       VarInfoName roots[] = new VarInfoName [args.length];
  //       for (int x = 0; x<args.length; x++)
  //         roots[x] = vars.get(args[x]);
  //       String result[] = VarInfoName.QuantHelper.format_jml(roots, true);
  //       for (int x = 0; x<result.length; x++)
  //         out.println(result[x]);
  //     }
  //   }
  //   static { handlers.put("quantify_format_jml_elem", new QuantifyFormatJMLElementwise()); }

  //   // public String[] QuantHelper.format_jml(VarInfoName[] roots, boolean elementwise,boolean
  // forall)
  //   private static class QuantifyFormatJMLExists implements Handler {
  //     public void handle(Map<String,VarInfoName> vars, String[] args, PrintStream out) {
  //       assertTrue(args.length >= 1);
  //       VarInfoName roots[] = new VarInfoName [args.length];
  //       for (int x = 0; x<args.length; x++)
  //         roots[x] = vars.get(args[x]);
  //       String result[] = VarInfoName.QuantHelper.format_jml(roots, false, false);
  //       for (int x = 0; x<result.length; x++)
  //         out.println(result[x]);
  //     }
  //   }
  //   static { handlers.put("quantify_format_jml_exists", new QuantifyFormatJMLExists()); }
}
