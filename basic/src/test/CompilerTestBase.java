package basic.src.test;

import basic.src.main.compiler.SupremeCompiler;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public abstract class CompilerTestBase {

    protected static String run(String source) throws Exception {
        return run(source, false);
    }

    protected static String run(String source, boolean optimized) throws Exception {
        String log = runLog(source, optimized);
        if (!log.contains("Exited with status: OK, Halted")) {
            fail("program did not halt cleanly. Raw log:\n" + log);
        }

        int marker = log.indexOf("----Running program");
        if (marker < 0)
            fail("no VM run banner. Raw log:\n" + log);
        int start = log.indexOf('\n', marker) + 1;

        int end = log.indexOf("----Program exited");
        if (end < 0)
            fail("no VM exit banner. Raw log:\n" + log);
        if (end > start && log.charAt(end - 1) == '\n')
            end--;
        if (end > start && log.charAt(end - 1) == '\r')
            end--;
        if (end < start)
            fail("malformed output. Raw log:\n" + log);

        return log.substring(start, end).replace("\r\n", "\n");
    }

    protected static String runBoth(String source) throws Exception {
        String plain = run(source, false);
        String optimized = run(source, true);
        assertEquals(plain, optimized, "optimizer changed program output");
        return plain;
    }

    protected static String runLog(String source, boolean optimized) throws Exception {
        Path root = repoRoot();
        Path vmExe = root
                .resolve(System.getProperty("os.name").toLowerCase().contains("win") ? "vm/main.exe" : "vm/main");

        if (!Files.exists(vmExe)) {
            fail("VM executable not found at " + vmExe
                    + ". Please run 'make' inside the vm/ directory before running tests.");
        }

        Path dir = Files.createTempDirectory("sbas");
        dir.toFile().deleteOnExit();
        String name = "t" + System.nanoTime();
        Path sasm = dir.resolve(name + ".sasm");
        sasm.toFile().deleteOnExit();

        SupremeCompiler compiler = new SupremeCompiler(1, false, optimized);
        assertTrue(compiler.compile(source, dir.resolve(name).toString()), "compilation failed");

        Process asm = new ProcessBuilder("python", root.resolve("assembler/asm.py").toString(), sasm.toString()).directory(repoRoot().toFile())
                .redirectErrorStream(true).start();

        asm.getOutputStream().close();
        String asmLog = new String(asm.getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);

        Path smc = root.resolve("executable").resolve(name + ".smc");
        assertTrue(Files.exists(smc), "assembler did not produce " + smc + "\n" + asmLog);

        Process vm = new ProcessBuilder(vmExe.toString(), smc.toString()).directory(root.toFile())
                .redirectErrorStream(true).start();
        vm.getOutputStream().close();
        byte[] raw = vm.getInputStream().readAllBytes();
        if (!vm.waitFor(30, TimeUnit.SECONDS)) {
            vm.destroyForcibly();
            fail("VM timed out");
        }
        smc.toFile().deleteOnExit();
        return asmLog + new String(raw, StandardCharsets.ISO_8859_1);
    }

    protected static void assertBytesEqual(byte[] expected, String actual) {
        assertBytesEqual(expected, actual, null);
    }

    protected static void assertBytesEqual(byte[] expected, String actual, String message) {
        byte[] actualBytes = actual.getBytes(StandardCharsets.ISO_8859_1);
        if (!Arrays.equals(expected, actualBytes)) {
            fail((message == null ? "" : message + ": ")
                    + "expected bytes " + formatBytes(expected) + " but was " + formatBytes(actualBytes));
        }
    }

    private static String formatBytes(byte[] values) {
        StringBuilder formatted = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                formatted.append(' ');
            }
            formatted.append(String.format("%02X", values[i] & 0xFF));
        }
        return formatted.append(']').toString();
    }

    protected static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = (byte) values[i];
        }
        return b;
    }

    private static Path repoRoot() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("assembler/asm.py"))) {
            p = p.getParent();
        }
        Assumptions.assumeTrue(p != null, "tests must run from inside the SupremeASM2 repo");
        return p;
    }
}