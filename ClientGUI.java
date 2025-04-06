import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientGUI {
    private JFrame frame;
    private JPanel messagePanel;
    private JTextField inputField;
    private JButton sendButton, renameButton, quitButton;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;

    public ClientGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 600);
        frame.setLayout(new BorderLayout());

        // Menu Panel with Rename and Quit
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        renameButton = new JButton("Rename");
        quitButton = new JButton("Quit");
        topPanel.add(renameButton);
        topPanel.add(quitButton);
        frame.add(topPanel, BorderLayout.NORTH);

        // Message display area
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(messagePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Input area
        inputField = new JTextField();
        sendButton = new JButton("Send");
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Action Listeners
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        renameButton.addActionListener(e -> rename());
        quitButton.addActionListener(e -> quit());
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            addMessage("You: " + message, true);
            out.println(message);
            out.flush();
            inputField.setText("");
        }
    }

    private void addMessage(String message, boolean isSender) {
        JPanel bubble = new JPanel(new BorderLayout());
        JLabel label = new JLabel("<html><div style='padding: 6px; max-width: 250px;'>" + message + "</div></html>");
        label.setOpaque(true);
        label.setBackground(isSender ? new Color(173, 216, 230) : new Color(240, 240, 240));
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    
        // Wrap bubble into a wrapper to control alignment
        JPanel wrapper = new JPanel(new FlowLayout(isSender ? FlowLayout.RIGHT : FlowLayout.LEFT));
        wrapper.setOpaque(false); // Transparent background
        wrapper.add(label);
    
        messagePanel.add(wrapper);
        messagePanel.revalidate();
        messagePanel.repaint();
    
        // Scroll to bottom automatically
        JScrollBar vertical = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
    

    private void rename() {
        String newName = JOptionPane.showInputDialog(frame, "Enter new nickname:", nickname);
        if (newName != null && !newName.trim().isEmpty()) {
            out.println("/name " + newName.trim());
        }
    }

    private void quit() {
        out.println("/quit");
        System.exit(0);
    }

    public void startClient(String serverAddress, int port) {
        try {
            Socket socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            nickname = JOptionPane.showInputDialog(frame, "Enter your nickname:", "Nickname", JOptionPane.QUESTION_MESSAGE);
            if (nickname != null && !nickname.trim().isEmpty()) {
                out.println(nickname);
                out.flush();
            }

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        final String finalMessage = message;
                        SwingUtilities.invokeLater(() -> addMessage(finalMessage, false));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            frame.setVisible(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        ClientGUI client = new ClientGUI();
        client.startClient("", 8080);
    }
}
