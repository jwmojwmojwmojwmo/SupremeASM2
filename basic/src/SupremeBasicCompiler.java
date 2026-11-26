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
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a valid SupremeBasicCompiler build command: <file.sbas> [--verbose] [--optimized]");
        String command = scanner.nextLine().trim();
        String[] parts = command.split(" ");
        String path = parts[0];
        boolean verbose = false;
        boolean optimized = false;
        for (int i = 1; i < parts.length; i++) {
            String arg = parts[i];
            if (arg.equals("--verbose") || arg.equals("-v")) {
                System.out.println("Verbose output enabled.");
                verbose = true;
            } else if (arg.equals("--optimized") || arg.equals("-o")) {
                System.out.println("Optimized output enabled.");
                optimized = true;
            } else {
                System.err.println("Unknown argument: " + arg);
                return;
            }
        }
        String content;
        try {
            if (!path.endsWith(".sbas")) {
                System.err.println("Error: file must be a .sbas file.");
                return;
            }
            content = Files.readString(Path.of(path));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String baseName = Path.of(path).getFileName().toString();
        SupremeCompiler compiler = new SupremeCompiler(1, verbose, optimized);
        baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        boolean success = compiler.compile(content, baseName);
        if (!success) {
            System.err.println("Compilation failed!");
            return;
        }
        System.out.println("Compilation successful!");
        System.out.println("Compile and run the generated SupremeASM using the SupremeASM compiler?");
        System.out.println(
                "Type 'y' to compile and run, 'ym' to compile and run with --dpm flag, 'yr' to compile and run with --dpr flag, 'ymr' to compile with both flags, or 'n' to exit.");
        String response = scanner.nextLine().trim().toLowerCase();
        scanner.close();
        if (response.equals("n"))
            return;

        List<String> asmArgs = new ArrayList<>(List.of("python", "assembler/asm.py", baseName + ".sasm"));
        switch (response) {
            case "y" -> asmArgs.add("--autorun");
            case "ym" -> {
                asmArgs.add("--autorun");
                asmArgs.add("--dpm");
            }
            case "yr" -> {
                asmArgs.add("--autorun");
                asmArgs.add("--dpr");
            }
            case "ymr" -> {
                asmArgs.add("--autorun");
                asmArgs.add("--dpm");
                asmArgs.add("--dpr");
            }
            default -> {
                return;
            }
        }
        try {
            Process p = new ProcessBuilder(asmArgs).inheritIO().start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}