package test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class TestJavaScript {
    private static final String script = "" +
                                         "var evaluate = function () {\n" +
                                         " return null;\n" +
                                         "};";
    public static void main(String... args) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        for (int i = 0; i < 1000; i++)
        {
            ScriptEngine scriptEngine = manager.getEngineByName("JavaScript");
            scriptEngine.eval(script);
            Invocable inv = (Invocable) scriptEngine;
            inv.invokeFunction("evaluate");
            System.out.println(i);
        }
        
        
    }
}