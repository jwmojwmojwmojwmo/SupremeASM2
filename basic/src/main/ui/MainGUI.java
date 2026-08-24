package basic.src.main.ui;

import javax.swing.*;

public class MainGUI {
    public static void main(String[] args) {
        JFrame frame = new GUIFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

    }
}