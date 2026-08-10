package basic.src.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Represents a driver handling compilation, assembly, and execution of SupremeBASIC code
public class SupremeDriver {
    public SupremeDriver() {

    }

    public void compile(String basicCode, String filename) {
        // In theory this regex splits semicolons accounting for quotes but who knows
        String[] lines = basicCode.trim().split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        StringBuilder asmCode = new StringBuilder();
        asmCode.append("@version 2\n\n");
        for (String line : lines) {
            asmCode.append(translateLine(line));
        }
        try {
            // Path.of handles the OS-specific slashes
            Files.writeString(Path.of(filename), asmCode.toString());
            System.out.println("Compiled code to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to write file: " + e.getMessage());
        }
    }

    public void assemble(String filename) {
        File sasmFile = new File(filename);
        String absoluteFilePath = sasmFile.getAbsolutePath();
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "asm.py", absoluteFilePath);
            pb.directory(new File("./assembler"));
            pb.inheritIO();
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                System.out.println("Assembly finished successfully.");
            } else {
                System.err.println("Process exited with error code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String run(String machineCode) {
        // Implementation for running machine code
        return ""; // Placeholder return
    }

    private String translateLine(String line) {
        String command = line.trim().split(" ")[0].toUpperCase();
        switch (command) {
            case "ASM":
                return line.substring(3).trim().replace("\"", "") + "\n";
            default:
                return "";
        }
    }
}
