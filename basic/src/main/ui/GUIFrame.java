package basic.src.main.ui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

public class GUIFrame extends JFrame {
    private final CodePanel editor;
    private final ConsolePanel console;
    private final String helpText = "placeholder";
    private Thread runThread;
    private boolean manuallyStopped = false;
    private final String NAME = "SupremeIDE";
    private final int NEW_WINDOW_OFFSET = 25;

    public GUIFrame() {
        super();
        setTitle(NAME);
        setSize(1200, 800);

        editor = new CodePanel();
        console = new ConsolePanel();
        createToolBar();

        add(editor, BorderLayout.CENTER);
        add(console, BorderLayout.SOUTH);

        //redirectSystemOut();
    }

    public GUIFrame(File file) throws IOException {
        this(); // This calls the constructor above and does all the UI work
        String code = Files.readString(file.toPath());
        editor.setCode(code);
        console.append("Loaded: " + file.getName());
        setTitle(NAME + ": " + file.getName());
    }

    private void createToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        JButton newBtn = new JButton("New");
        newBtn.setToolTipText("Create a new window");
        newBtn.addActionListener(e -> newFile());
        JButton saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save code to a file");
        saveBtn.addActionListener(e -> saveFile());
        JButton openBtn = new JButton("Open");
        openBtn.setToolTipText("Open a saved file");
        openBtn.addActionListener(e -> openFile());
        JButton helpBtn = new JButton("Help");
        helpBtn.setToolTipText("Open the documentation");
        helpBtn.addActionListener(e -> showHelp());
        JButton byteBtn = new JButton("See Bytecode");
        byteBtn.setToolTipText("Displays bytecode of current ASM program");
        byteBtn.addActionListener(e -> showByte());
        // JButton resetBtn = new JButton("Resync I/O");
        // resetBtn.setToolTipText("Resyncs input and output to current console if
        // broken");
        // resetBtn.addActionListener(e -> redirectSystemOut());
        JButton runBtn = new JButton("Run");
        runBtn.setToolTipText("Compile and Run");
        runBtn.setBackground(new Color(0, 150, 0));
        runBtn.setForeground(Color.WHITE);
        runBtn.addActionListener(e -> run());
        JButton stopBtn = new JButton("Stop");
        stopBtn.setToolTipText("Halt Execution");
        stopBtn.setBackground(new Color(150, 0, 0));
        stopBtn.setForeground(Color.WHITE);
        stopBtn.addActionListener(e -> stop());
        JButton clearBtn = new JButton("Clear");
        clearBtn.setToolTipText("Clear Console Output");
        clearBtn.addActionListener(e -> console.clear());
        toolbar.add(newBtn);
        toolbar.add(saveBtn);
        toolbar.add(openBtn);
        toolbar.add(helpBtn);
        toolbar.add(byteBtn);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(runBtn);
        toolbar.add(stopBtn);
        toolbar.addSeparator();
        toolbar.add(clearBtn);
        toolbar.addSeparator();
        add(toolbar, BorderLayout.NORTH);
    }

    private void run() {
        manuallyStopped = false;
        // redirectSystemIn();
        // redirectSystemOut();
        console.clear();
        runThread = new Thread(() -> {
            // TODO
            // driver.assemble("temp.sasm");
            // driver.run("temp.smc");
        });
        runThread.start();
    }

    private String getCleanCode(String code) {
        StringBuilder cleanCode = new StringBuilder();
        String[] lines = code.split("\n");
        for (String line : lines) {
            if (line.contains("//")) {
                line = line.substring(0, line.indexOf("//"));
            }
            cleanCode.append(line).append("\n");
        }
        return cleanCode.toString();
    }

    private void stop() {
        if (runThread != null && runThread.isAlive()) {
            runThread.interrupt();
        }
        manuallyStopped = true;
    }

    private void newFile() {
        JFrame frame = new GUIFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(this);
        frame.setLocation(frame.getX() + NEW_WINDOW_OFFSET, frame.getY() + NEW_WINDOW_OFFSET);
        frame.setVisible(true);
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./scripts"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("SupremeBASIC Files", "sbasic", "txt"));

        int result = fileChooser.showOpenDialog(editor);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            try {
                JFrame frame = new GUIFrame(selected);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(this);
                frame.setLocation(frame.getX() + NEW_WINDOW_OFFSET, frame.getY() + NEW_WINDOW_OFFSET);
                frame.setVisible(true);
                // String code = Files.readString(selected.toPath());
                // editor.setCode(code);
                // console.append("Loaded: " + selected.getName());
                // this.setTitle(name + ": " + selected.getName());
            } catch (Exception e) {
                console.append("Error: " + e.getMessage());
            }
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./scripts")); // Start in default scripts folder
        // Filter for .sasm files
        fileChooser.setFileFilter(new FileNameExtensionFilter("SupremeASM Files", "sasm"));
        int result = fileChooser.showSaveDialog(editor);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            // Auto-add extension if missing
            if (!selected.getName().toLowerCase().endsWith(".sasm")) {
                selected = new File(selected.getAbsolutePath() + ".sasm");
            }
            try {
                Files.writeString(selected.toPath(), editor.getCode());
                console.append("Saved: " + selected.getName());
                this.setTitle(NAME + ": " + selected.getName());
            } catch (Exception e) {
                console.append("Error: " + e.getMessage());
            }
        }
    }

    private void showHelp() {
        JDialog helpDialog = new JDialog(this, "SupremeASM Help", false);
        JEditorPane helpPane = new JEditorPane("text/html", helpText);
        helpPane.setEditable(false);
        helpPane.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(helpPane);
        helpDialog.add(scrollPane);
        helpDialog.pack();
        helpDialog.setSize(500, 1000);
        helpDialog.setVisible(true);
    }

    private void showByte() {
        String code = getCleanCode(editor.getCode());
        byte[] bytecode;
        JDialog dialog = new JDialog(this, "Machine Code Viewer", false);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(460, 300);
        JTextArea hexArea = new JTextArea();
        hexArea.setEditable(false);
        hexArea.setFont(new Font("Monospaced", java.awt.Font.PLAIN, 14));
        hexArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(hexArea);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        hexArea.setText("Compiling...");
        try {
            // TODO
        } catch (Exception e) {
            hexArea.setText("Code compilation failed starting at line @" + e);
        }

    }

    private String formatBytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X", data[i]));
            sb.append(" ");
            if ((i + 1) % 16 == 0) { // new line every 16 bytes
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void redirectSystemOut() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                console.appendWithoutFormatting(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                String text = new String(b, off, len);
                console.appendWithoutFormatting(text);
            }
        };
        PrintStream newStream = new PrintStream(out, true); // true = auto-flush
        System.setOut(newStream); // Capture normal System.out.println
    }

    private void redirectSystemIn() {
        PipedOutputStream guiWriter = new PipedOutputStream();
        PipedInputStream systemReader = null;
        try {
            systemReader = new PipedInputStream(guiWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.setIn(systemReader);
        console.setInputPipe(guiWriter);
    }
}
