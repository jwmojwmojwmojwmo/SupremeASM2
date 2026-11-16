package basic.src;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import basic.src.main.compiler.SupremeCompiler;

public class SupremeBasicCompiler {
    public static void main(String[] args) {
        SupremeCompiler compiler = new SupremeCompiler(1);
        String content;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter file path to SupremeBASIC program:");
        String path = scanner.nextLine();
        try {
            Path filePath = Path.of(path);
            content = Files.readString(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        boolean success = compiler.compile(content, "test");
        if (!success) {
            System.err.println("Compilation failed!");
            return;
        }
        System.out.println("Compilation successful!");
        System.out.println("Compile and run the generated SupremeASM using the SupremeASM compiler?");
        System.out.println("Type 'y' to compile and run, 'ym' to compile and run with --dpm flag, 'yr' to compile and run with --dpr flag, 'ymr' to compile and run with both flags, or 'n' to exit.");
        String response = scanner.nextLine();
        scanner.close();
        if (response.equalsIgnoreCase("n")) 
            return;
        List<String> asmArgs = new ArrayList<>(List.of("python", "assembler/asm.py", path.split("\\.")[0] + ".sasm"));
        ProcessBuilder pb;
        switch (response.toLowerCase()) {
            case "y" -> pb = new ProcessBuilder(asmArgs);
            case "ym" -> {
                asmArgs.add("--autorun");
                asmArgs.add("--dpm");
                pb = new ProcessBuilder(asmArgs);
            }
            case "yr" -> {
                asmArgs.add("--autorun");
                asmArgs.add("--dpr");
                pb = new ProcessBuilder(asmArgs);
            }
            case "ymr" -> {
                asmArgs.add("--autorun");
                asmArgs.add("--dpm");
                asmArgs.add("--dpr");
                pb = new ProcessBuilder(asmArgs);
            }
            default -> {
                return;
            }
        }
        try {
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
