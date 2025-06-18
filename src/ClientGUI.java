import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Base64;

public class ClientGUI extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final String SERVER_VERSION_FILE = "server_version.txt";
    private static final String CLIENT_JAR = "Client.jar";
    private static final String TEMP_CLIENT_JAR = "Client_temp.jar";

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;

    public ClientGUI() {
        super("文件传输客户端");
        initializeUI();
        connectToServer();
        checkServerVersion(); // 新增：检查服务器版本
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

    // 新增：检查服务器版本
    private void checkServerVersion() {
        try {
            String serverVersionResponse = in.readLine();
            if (serverVersionResponse != null && serverVersionResponse.startsWith("VERSION")) {
                String serverVersion = serverVersionResponse.split(" ")[1];
                String localServerVersion = getLocalServerVersion();

                if (isNewerVersion(serverVersion, localServerVersion)) {
                    int option = JOptionPane.showConfirmDialog(this,
                            "发现新服务器版本: " + serverVersion + "\n是否立即更新客户端?",
                            "客户端更新", JOptionPane.YES_NO_OPTION);

                    if (option == JOptionPane.YES_OPTION) {
                        downloadNewClient(serverVersion);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLocalServerVersion() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SERVER_VERSION_FILE))) {
            return reader.readLine();
        } catch (IOException e) {
            return "0";
        }
    }

    private boolean isNewerVersion(String newVersion, String oldVersion) {
        try {
            int newVer = Integer.parseInt(newVersion);
            int oldVer = Integer.parseInt(oldVersion);
            return newVer > oldVer;
        } catch (NumberFormatException e) {
            return newVersion.compareTo(oldVersion) > 0;
        }
    }

    private void downloadNewClient(String serverVersion) {
        try {
            out.println("DOWNLOAD_CLIENT");
            String response = in.readLine();
            if ("ERROR".equals(response)) {
                JOptionPane.showMessageDialog(this, "下载失败：服务器端无客户端文件", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            byte[] newClientBytes = Base64.getDecoder().decode(response);
            Path tempPath = Paths.get(TEMP_CLIENT_JAR);
            Files.write(tempPath, newClientBytes);

            // 替换旧客户端
            File oldClient = new File(CLIENT_JAR);
            if (oldClient.exists()) {
                Files.move(tempPath, Paths.get(CLIENT_JAR), StandardCopyOption.REPLACE_EXISTING);
                saveLocalServerVersion(serverVersion);

                JOptionPane.showMessageDialog(this, "客户端更新成功，即将重启");
                restartClient();
            } else {
                JOptionPane.showMessageDialog(this, "警告：未找到当前客户端文件", "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "更新失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void saveLocalServerVersion(String version) {
        try (FileWriter writer = new FileWriter(SERVER_VERSION_FILE)) {
            writer.write(version);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restartClient() {
        try {
            Runtime.getRuntime().exec("java -jar " + CLIENT_JAR);
            System.exit(0);
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