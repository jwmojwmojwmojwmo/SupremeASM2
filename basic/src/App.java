package basic.src;

import basic.src.main.compiler.SupremeCompiler;

public class App {
    public static void main(String[] args) {
        SupremeCompiler compiler = new SupremeCompiler(1);
        compiler.compile("DECLARE x SIZE 4;\n x <- 5;  // hi");
    }
}
