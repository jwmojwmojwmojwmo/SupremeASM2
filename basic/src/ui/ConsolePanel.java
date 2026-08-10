package basic.src.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.OutputStream;

public class ConsolePanel extends JPanel {
    private final JTextArea console;
    private OutputStream inputPipe;
    private int lastPromptPos = 0;

    public ConsolePanel() {
        setLayout(new BorderLayout());
        console = new JTextArea(20, 50);
        console.setBackground(Color.BLACK);
        console.setForeground(Color.WHITE);
        console.setFont(new Font("Monospaced", Font.BOLD, 14));
        JScrollPane scrollPane = new JScrollPane(console);
        add(scrollPane, BorderLayout.CENTER);
        console.setText("> Welcome to SupremeIDE v1.0");
        updateCaretAndPosition();
        console.setEditable(true);
        console.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (console.getCaretPosition() <= lastPromptPos) {
                        e.consume(); // Stop the backspace
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendUserInput();
                }
            }
        });
    }

    public void setInputPipe(OutputStream pipe) {
        this.inputPipe = pipe;
    }

    private void sendUserInput() {
        if (inputPipe == null) return;
        try {
            String text = console.getText().substring(lastPromptPos);
            inputPipe.write((text + "\n").getBytes());
            inputPipe.flush();
            updateCaretAndPosition();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void append(String text) {
        safeUpdate(() -> {
            console.append("\n> " + text);
            updateCaretAndPosition();
        });
    }

    public void appendWithoutFormatting(String text) {
        safeUpdate(() -> {
            console.append(text);
            updateCaretAndPosition();
        });
    }

    public void clear() {
        safeUpdate(() -> {
            console.setText("");
            lastPromptPos = 0;
        });
    }

    private void safeUpdate(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(action);
            } catch (Exception e) {
            }
        }
    }

    private void updateCaretAndPosition() {
        console.setCaretPosition(console.getDocument().getLength());
        lastPromptPos = console.getDocument().getLength();
    }
}

