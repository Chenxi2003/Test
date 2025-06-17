import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Base64;

public class ClientGUI extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    public ClientGUI() {
        super("文件传输客户端");
        initializeUI();
        connectToServer();
    }

    private void initializeUI() {
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

        JButton uploadButton = new JButton("上传文件");
        JButton exitButton = new JButton("退出");

        uploadButton.addActionListener(e -> uploadFile());
        exitButton.addActionListener(e -> {
            try {
                out.println("EXIT");
                System.exit(0);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        panel.add(uploadButton, BorderLayout.CENTER);
        panel.add(exitButton, BorderLayout.SOUTH);

        add(panel);
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("已连接到服务器");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] fileContent = new byte[(int) selectedFile.length()];
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    fis.read(fileContent);
                }
                String fileContentBase64 = Base64.getEncoder().encodeToString(fileContent);

                // 发送上传请求
                out.println("UPLOAD");
                out.println(selectedFile.getName());
                out.println(selectedFile.length());
                out.println(fileContentBase64);

                // 接收服务器响应
                String response = in.readLine();
                if ("UPLOAD_SUCCESS".equals(response)) {
                    JOptionPane.showMessageDialog(this, "文件上传成功：" + selectedFile.getName());
                } else {
                    JOptionPane.showMessageDialog(this, "文件上传失败：" + selectedFile.getName(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "文件读取失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientGUI().setVisible(true);
        });
    }

    @Override
    public void dispose() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }
}
