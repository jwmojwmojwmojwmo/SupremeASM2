package basic.src.main.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;

public class CodePanel extends JPanel {
    private final RSyntaxTextArea codeArea;

    public CodePanel() {
        setLayout(new BorderLayout());
        // main code area
        codeArea = new RSyntaxTextArea(20, 60);
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(false);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        RTextScrollPane scrollPane = new RTextScrollPane(codeArea);
        scrollPane.setFoldIndicatorEnabled(true);
        add(scrollPane, BorderLayout.CENTER);
    }

    public String getCode() {
        return codeArea.getText();
    }

    public void setCode(String code) {
        codeArea.setText(code);
        codeArea.setCaretPosition(0); // Scroll to top
    }
}
