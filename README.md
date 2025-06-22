# 项目训练报告-周晨翔

## 一、项目信息

* **课程名称** ：嵌入式软件设计
* **所属院系** ：计算机工程学院
* **专业** ：计算机科学与技术
* **班级** ：计算机 222
* **学生姓名** ：周晨翔
* **学号** ：202220232
* **设计地点** ：信息楼 A115
* **指导教师** ：曹欲晓
* **设计时间** ：2025 年 6 月 9 日至 2025 年 6 月 20 日

## 二、项目训练目的及意义

本课程综合数据结构、操作系统、程序设计、计算机网络、软件工程等相关专业课程的理论知识，通过项目驱动的方式，将理论知识与联系实际在一起，通过综合性的项目训练，培养学生独立思考、解决实际工程问题的能力；提高学生设计、实现、调试、测试软件系统的能力。

## 三、设计任务

1. 掌握应用 SOCKET 编写网络程序的原理和方法。
2. 掌握 BASE64 编码的原理和编码解码方法。
3. 掌握通过多线程（多进程）实现多任务的方法。
4. 掌握程序自动升级的原理和实现方法。
5. 掌握使用 LSB(Least Significant Bit) 技术实现对 24 位位图进行信息隐写的原理和实现方法（选做）。
6. 掌握 Git 工具的使用方法（自学）。
7. 掌握使用 Markdown 标记语言撰写技术文档的方法（自学）。
8. 撰写规范的设计总结报告，培养严谨的作风和科学的态度。
9. 掌握 Sqlite 数据库的移植和使用。

## 四、设计课题内容要求

1. 实现客户端和服务端的 TCP 通信（选做：服务端实现多路转接）。
2. Client 程序运行后时，启动多线程，一个主线程用来实现和用户交互，另外根据需要启动工作线程。
3. Client 启动时首先检测 Server 端是否有版本升级标志，如果有则自动下载客户端程序，下次启动时启动新的 Client 程序。
4. Client 上传 Server 的所有数据用 Base64 编码后上传，Server 端接收后解码进行存储（文件直接存储，字段数据用 Sqlite 数据库存储）。
5. 文件以及数据的传输用 CRC32 进行校验（选做）。
6. Server 收到上传的 BMP 文件以及文字信息，用 LSB 隐写技术把文字信息隐藏进图像文件，并且可以考虑用随机算法每次产生不同的隐写位置（选做）。
7. Client 收到 Server 返回的文件后，检查 BMP 是否采用了 LSB 隐写，如果有隐写信息，则解析出隐写信息（选做）。
8. 所有代码必须用 Git 进行版本控制，代码和文档提交在 Github 或者 Gitee。
9. 文档必须用 Markdown 编写，Markdown 编辑器推荐使用 MarkdownPad。
10. 建议做图形界面的应用程序。
11. Git , Markdown 的教程自行网上学习。

## 五、设计流程及设计思想说明

### （一）整体架构设计

本项目采用客户端 - 服务端（C/S）架构，客户端负责文件的读取、编码以及与用户的交互，服务端负责接收、解码、存储文件，并提供版本更新等功能。两者通过 TCP 协议进行通信，确保数据传输的可靠性。

### （二）模块划分与功能设计

1. **服务端模块** ：
    * **多线程处理模块** ：每当有客户端连接时，服务端会创建一个新的线程来处理该客户端的请求，使得多个客户端可以同时连接并上传文件，提高了服务器的并发处理能力。
    * **文件接收与存储模块** ：接收客户端发送的 BASE64 编码后的文件数据，解码后根据文件类型进行存储。对于普通文件，直接存储到指定目录；对于 BMP 文件，若带有隐写消息，则调用隐写模块进行处理后再存储。
    * **版本管理模块** ：读取服务器端的版本文件，当客户端请求连接时，将版本信息发送给客户端，以便客户端判断是否需要更新。同时，提供下载新客户端的功能。
    * **数据库操作模块** ：使用 Sqlite 数据库存储文件的相关信息，如文件名、文件大小、CRC32 校验值等，方便对文件进行管理和查询。

2. **客户端模块** ：
    * **用户交互模块** ：通过命令行或图形界面接收用户的指令，如上传文件、退出等，并显示相应的结果和提示信息。
    * **文件上传模块** ：读取本地文件，将其进行 BASE64 编码后发送给服务端。在上传 BMP 文件时，根据用户输入决定是否嵌入隐写消息。
    * **版本检查与更新模块** ：连接到服务端后，获取服务器版本信息，与本地版本进行比较。如果服务器版本更新，则下载新的客户端程序并替换旧版本，实现自动更新功能。

### （三）编码与传输设计

1. **BASE64 编码** ：客户端在上传文件时，先将文件内容读取为字节数组，然后使用 Java 内置的 Base64 编码器对其进行编码，将二进制数据转换为 ASCII 字符串，以便通过网络以文本形式传输。服务端接收到编码后的数据后，使用对应的解码器还原出原始的文件字节数组。
2. **TCP 传输** ：利用 Java 的 Socket 类建立客户端与服务端之间的 TCP 连接。客户端通过 Socket 将编码后的文件数据发送给服务端，服务端通过监听端口接收数据。TCP 协议保证了数据传输的可靠性，确保文件能够完整、准确地从客户端传输到服务端。

### （四）隐写技术设计

1. **LSB 隐写原理** ：对于 BMP 文件的隐写，采用 LSB（Least Significant Bit，最低有效位）技术。将要嵌入的消息转换为二进制比特流，然后依次替换 BMP 图像像素值的最低有效位。由于最低有效位的改变对像素值的影响较小，因此在视觉上很难察觉到图像的变化，从而实现信息的隐藏。
2. **隐写实现流程** ：在客户端，当用户需要上传 BMP 文件并嵌入消息时，客户端将消息转换为字节数组，并在每个字节后添加结束标志。然后，从 BMP 文件头之后的像素数据开始，依次将消息的比特替换到像素值的最低有效位。服务端接收到嵌有消息的 BMP 文件后，若该文件需要隐写，则在存储前进一步处理，如对消息进行加密等增强隐写安全性。

### （五）自动更新设计

1. **版本检查机制** ：客户端在启动时，首先连接到服务端，服务端将自身的版本信息（存储在 version.txt 文件中）发送给客户端。客户端读取本地的版本信息（存储在 server_version.txt 文件中）进行比较。如果服务端版本大于客户端本地保存的服务端版本，则提示用户有新的客户端版本可用。
2. **更新实现流程** ：用户确认更新后，客户端向服务端发送下载新客户端的请求。服务端读取客户端程序文件（Client.jar），将其进行 BASE64 编码后发送给客户端。客户端接收到编码后的数据，解码并保存为新的客户端程序文件，替换旧版本文件，然后重新启动新的客户端程序，完成自动更新过程。

## 六、程序清单

### （一）FileServer.java

```java
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.zip.CRC32;
import java.sql.*;

public class FileServer {
    private static final int PORT = 8888;
    private static final String VERSION_FILE = "version.txt";
    private static final String CLIENT_DIR = "client_dir/";
    private static final String FILE_DIR = "file_dir/";
    private static final String DB_PATH = "file_info.db";
    private static final String CLIENT_JAR = "Client.jar";

    public static void main(String[] args) {
        new File(CLIENT_DIR).mkdirs();
        new File(FILE_DIR).mkdirs();
        initDatabase();
        System.out.println("当前工作目录: " + System.getProperty("user.dir"));

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("服务器已启动，等待客户端连接...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("一个客户端已连接");
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            if (conn != null) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS files (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "filename TEXT NOT NULL, " +
                        "filelength INTEGER NOT NULL, " +
                        "crc32 BIGINT NOT NULL, " +
                        "upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
                try (PreparedStatement pstmt = conn.prepareStatement(createTableSQL)) {
                    pstmt.execute();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void saveFileInfoToDB(String fileName, long fileLength, long crc32Value) {
        String sql = "INSERT INTO files(filename, filelength, crc32) VALUES(?, ?, ?)";
        System.out.println("正在保存文件信息到数据库：" + fileName);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileName);
            pstmt.setLong(2, fileLength);
            pstmt.setLong(3, crc32Value);
            pstmt.executeUpdate();
            System.out.println("文件信息已存入数据库：" + fileName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            ) {
                checkClientVersion(out);

                String command;
                while ((command = in.readLine()) != null) {
                    if (command.startsWith("UPLOAD")) {
                        handleUpload(in, out);
                    } else if (command.startsWith("DOWNLOAD_CLIENT")) {
                        handleDownloadClient(out);
                    } else if (command.startsWith("EXIT")) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void checkClientVersion(PrintWriter out) {
            File versionFile = new File(VERSION_FILE);
            if (versionFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(versionFile))) {
                    String version = reader.readLine();
                    out.println("VERSION " + version);
                } catch (IOException e) {
                    e.printStackTrace();
                    out.println("VERSION 0");
                }
            } else {
                out.println("VERSION 0");
            }
        }

        private void handleDownloadClient(PrintWriter out) {
            File clientJar = new File(FILE_DIR + CLIENT_JAR);
            if (!clientJar.exists()) {
                out.println("ERROR");
                return;
            }

            try (FileInputStream fis = new FileInputStream(clientJar)) {
                byte[] fileContent = new byte[(int) clientJar.length()];
                fis.read(fileContent);
                String base64Content = Base64.getEncoder().encodeToString(fileContent);
                out.println(base64Content);
                System.out.println("客户端JAR文件已发送");
            } catch (IOException e) {
                e.printStackTrace();
                out.println("ERROR");
            }
        }

        private void handleUpload(BufferedReader in, PrintWriter out) throws IOException {
            String fileName = in.readLine();
            long fileLength = Long.parseLong(in.readLine());
            String fileContentBase64 = in.readLine();
            // 读取要嵌入的消息
            String messageToEmbed = in.readLine();

            byte[] fileContent = Base64.getDecoder().decode(fileContentBase64);

            // 计算原始文件的CRC32校验值
            CRC32 crc32 = new CRC32();
            crc32.update(fileContent);
            long originalCrc32 = crc32.getValue();

            byte[] finalFileContent = fileContent;
            long finalCrc32 = originalCrc32;

            // 如果文件是BMP且有要嵌入的消息，则进行隐写
            if (fileName.toLowerCase().endsWith(".bmp") && messageToEmbed != null && !messageToEmbed.isEmpty()) {
                byte[] stegoContent = LSBSteganographyEmbedder.embedMessage(fileContent, messageToEmbed);
                if (stegoContent != null) {
                    finalFileContent = stegoContent;
                    // 重新计算CRC32
                    crc32.reset();
                    crc32.update(finalFileContent);
                    finalCrc32 = crc32.getValue();
                    System.out.println("已将消息嵌入到BMP文件中: " + messageToEmbed);
                }
            }

            if (checkFileIntegrity(finalFileContent, fileLength)) {
                saveFile(fileName, finalFileContent);
                FileServer.saveFileInfoToDB(fileName, fileLength, finalCrc32);

                // 返回上传成功和隐写后的文件内容
                out.println("UPLOAD_SUCCESS");
                out.println(Base64.getEncoder().encodeToString(finalFileContent));
                System.out.println("文件上传成功：" + fileName);
            } else {
                out.println("UPLOAD_FAILED");
                System.out.println("文件上传失败，校验失败：" + fileName);
            }
        }

        private boolean checkFileIntegrity(byte[] fileContent, long expectedLength) {
            return fileContent.length == expectedLength;
        }

        private void saveFile(String fileName, byte[] fileContent) {
            try (FileOutputStream fos = new FileOutputStream(FILE_DIR + fileName)) {
                fos.write(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

### （二）FileClient.java

```java
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.Scanner;

public class FileClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final String VERSION_FILE = "version.txt";
    private static final String SERVER_VERSION_FILE = "server_version.txt";
    private static final String CLIENT_JAR = "Client.jar";
    private static final String TEMP_CLIENT_JAR = "Client_temp.jar";

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        new FileClient().start();
    }

    public void start() {
        try {
            connectToServer();
            checkServerVersionUpdate();
            checkVersionUpdate();

            Thread interactionThread = new Thread(new InteractionTask());
            interactionThread.start();
            interactionThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void connectToServer() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("已连接到服务器");
    }

    private void checkServerVersionUpdate() throws IOException {
        String serverVersionResponse = in.readLine();
        if (serverVersionResponse != null && serverVersionResponse.startsWith("VERSION")) {
            String serverVersion = serverVersionResponse.split(" ")[1];
            String localServerVersion = getLocalServerVersion();

            if (isNewerVersion(serverVersion, localServerVersion)) {
                System.out.println("发现新服务器版本: " + serverVersion + " (当前: " + localServerVersion + ")");
                System.out.println("开始更新客户端...");
                if (downloadNewClient()) {
                    saveLocalServerVersion(serverVersion);
                    restartClient();
                }
            } else {
                System.out.println("服务器版本已是最新: " + serverVersion);
                saveLocalServerVersion(serverVersion);
            }
        }
    }

    private String getLocalServerVersion() {
        try (BufferedReader reader = new BufferedReader(new FileReader(SERVER_VERSION_FILE))) {
            return reader.readLine();
        } catch (IOException e) {
            return "0";
        }
    }

    private void saveLocalServerVersion(String version) {
        try (FileWriter writer = new FileWriter(SERVER_VERSION_FILE)) {
            writer.write(version);
        } catch (IOException e) {
            e.printStackTrace();
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

    private boolean downloadNewClient() {
        try {
            out.println("DOWNLOAD_CLIENT");
            String response = in.readLine();
            if ("ERROR".equals(response)) {
                System.out.println("下载失败：服务器端无客户端文件");
                return false;
            }

            byte[] newClientBytes = Base64.getDecoder().decode(response);
            Path tempPath = Paths.get(TEMP_CLIENT_JAR);
            Files.write(tempPath, newClientBytes);

            File oldClient = new File(CLIENT_JAR);
            if (oldClient.exists()) {
                Files.move(tempPath, Paths.get(CLIENT_JAR), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("客户端更新成功");
                return true;
            } else {
                System.out.println("警告：未找到当前客户端文件，更新未完成");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void restartClient() {
        try {
            System.out.println("重启客户端...");
            Runtime.getRuntime().exec("java -jar " + CLIENT_JAR);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkVersionUpdate() throws IOException {
        String version = getVersion();
        out.println("CLIENT_VERSION " + version);

        String response = in.readLine();
        if (response != null && response.startsWith("NEW_VERSION_AVAILABLE")) {
            String newVersion = response.split(" ")[1];
            downloadNewVersion(newVersion);
            System.out.println("新的客户端版本可用，已下载并替换旧版本");
            System.exit(0);
        }
    }

    private String getVersion() {
        File versionFile = new File(VERSION_FILE);
        if (versionFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(versionFile))) {
                return reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "0";
    }

    private void downloadNewVersion(String newVersion) {
        try (FileWriter writer = new FileWriter(VERSION_FILE)) {
            writer.write(newVersion);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class InteractionTask implements Runnable {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("请输入命令（UPLOAD 文件名 或 EXIT 退出）：");
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("EXIT")) {
                    try {
                        out.println("EXIT");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                } else if (command.startsWith("UPLOAD")) {
                    String fileName = command.split(" ")[1];
                    uploadFile(fileName);
                }
            }
            scanner.close();
        }
    }

    private void uploadFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("文件不存在：" + fileName);
            return;
        }

        try {
            byte[] fileContent = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileContent);
            }
            String fileContentBase64 = Base64.getEncoder().encodeToString(fileContent);

            // 如果是BMP文件，询问用户要嵌入的消息
            String messageToEmbed = "";
            if (fileName.toLowerCase().endsWith(".bmp")) {
                System.out.println("请输入要嵌入到BMP文件中的消息（直接回车则不嵌入）：");
                Scanner scanner = new Scanner(System.in);
                messageToEmbed = scanner.nextLine();
            }

            // 发送上传请求
            out.println("UPLOAD");
            out.println(fileName);
            out.println(file.length());
            out.println(fileContentBase64);
            out.println(messageToEmbed); // 发送要嵌入的消息

            // 接收服务器响应
            String response = in.readLine();
            if ("UPLOAD_SUCCESS".equals(response)) {
                // 读取服务器返回的隐写文件内容
                String stegoFileContentBase64 = in.readLine();
                byte[] stegoFileContent = Base64.getDecoder().decode(stegoFileContentBase64);

                System.out.println("文件上传成功：" + fileName);

                // 检查服务器返回的文件是否包含LSB隐写
                if (fileName.toLowerCase().endsWith(".bmp")) {
                    String hiddenMessage = LSBSteganographyAnalyzer.extractLSBMessage(stegoFileContent);
                    if (hiddenMessage != null) {
                        System.out.println("服务器处理后的文件包含LSB隐写信息: " + hiddenMessage);
                    } else {
                        System.out.println("服务器处理后的文件未发现LSB隐写信息");
                    }
                }
            } else {
                System.out.println("文件上传失败：" + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### （三）ClientGUI.java

```java
import javax.swing.*;
import java.awt.*;
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
        checkServerVersion();
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

                // 如果是BMP文件，询问用户要嵌入的消息
                String messageToEmbed = "";
                if (selectedFile.getName().toLowerCase().endsWith(".bmp")) {
                    messageToEmbed = JOptionPane.showInputDialog(this,
                            "请输入要嵌入到BMP文件中的消息",
                            "隐写消息",
                            JOptionPane.QUESTION_MESSAGE);
                    if (messageToEmbed == null) {
                        messageToEmbed = ""; // 用户取消
                    }
                }

                // 发送上传请求
                out.println("UPLOAD");
                out.println(selectedFile.getName());
                out.println(selectedFile.length());
                out.println(fileContentBase64);
                out.println(messageToEmbed); // 发送要嵌入的消息

                // 接收服务器响应
                String response = in.readLine();
                if ("UPLOAD_SUCCESS".equals(response)) {
                    // 读取服务器返回的隐写文件内容
                    String stegoFileContentBase64 = in.readLine();
                    byte[] stegoFileContent = Base64.getDecoder().decode(stegoFileContentBase64);

                    JOptionPane.showMessageDialog(this, "文件上传成功：" + selectedFile.getName());

                    // 检查服务器返回的文件是否包含LSB隐写
                    if (selectedFile.getName().toLowerCase().endsWith(".bmp")) {
                        String hiddenMessage = LSBSteganographyAnalyzer.extractLSBMessage(stegoFileContent);

                        if (hiddenMessage != null) {
                            JOptionPane.showMessageDialog(this,
                                    "服务器处理后的文件包含LSB隐写信息: " + hiddenMessage,
                                    "隐写分析结果",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "服务器处理后的文件未发现LSB隐写信息",
                                    "隐写分析结果",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
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
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.dispose();
    }
}
```

### （四）LSBSteganographyEmbedder.java

```java
public class LSBSteganographyEmbedder {
    private static final int BMP_HEADER_SIZE = 54;
    private static final int MAX_EMBED_SIZE = 1000000;

    public static byte[] embedMessage(byte[] imageData, String message) {
        if (!isBMPFile(imageData)) {
            return null;
        }
        if (imageData.length <= BMP_HEADER_SIZE) {
            return null;
        }

        byte[] messageBytes = message.getBytes();
        byte[] dataToEmbed = new byte[messageBytes.length + 1];
        System.arraycopy(messageBytes, 0, dataToEmbed, 0, messageBytes.length);
        dataToEmbed[messageBytes.length] = 0; // 结束标志

        int bitsNeeded = dataToEmbed.length * 8;
        int availablePixels = imageData.length - BMP_HEADER_SIZE;

        if (bitsNeeded > availablePixels || bitsNeeded > MAX_EMBED_SIZE) {
            return null;
        }

        byte[] stegoImage = imageData.clone();

        int bitIndex = 0;
        for (int pos = 0; pos < availablePixels; pos++) {
            if (bitIndex >= bitsNeeded) break;

            int actualPos = BMP_HEADER_SIZE + pos;
            byte currentByte = stegoImage[actualPos];
            int bitToEmbed = (dataToEmbed[bitIndex / 8] >> (7 - (bitIndex % 8))) & 1;
            currentByte = (byte) ((currentByte & 0xFE) | bitToEmbed);
            stegoImage[actualPos] = currentByte;
            bitIndex++;
        }
        return stegoImage;
    }

    private static boolean isBMPFile(byte[] fileContent) {
        return fileContent.length > 2 &&
                fileContent[0] == 'B' &&
                fileContent[1] == 'M';
    }
}
```

### （五）LSBSteganographyAnalyzer.java

```java
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class LSBSteganographyAnalyzer {
    private static final int BMP_HEADER_SIZE = 54;
    private static final int MAX_EMBED_SIZE = 1000000; // 最大分析1MB数据

    public static String extractLSBMessage(byte[] fileContent) throws IOException {
        if (!isBMPFile(fileContent)) {
            return null;
        }
        if (fileContent.length <= BMP_HEADER_SIZE) {
            return null;
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
        inputStream.skip(BMP_HEADER_SIZE);

        StringBuilder bitString = new StringBuilder();
        int bytesProcessed = 0;
        int maxBytesToProcess = Math.min(MAX_EMBED_SIZE, fileContent.length - BMP_HEADER_SIZE);
        while (bytesProcessed < maxBytesToProcess) {
            int pixelByte = inputStream.read();
            if (pixelByte == -1) break;
            bitString.append(pixelByte & 0x01);
            bytesProcessed++;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < bitString.length(); i += 8) {
            if (i + 8 > bitString.length()) break;
            String byteStr = bitString.substring(i, i + 8);
            int charCode = Integer.parseInt(byteStr, 2);
            if (charCode == 0) break;
            message.append((char) charCode);
        }
        return message.length() > 0 ? message.toString() : null;
    }

    private static boolean isBMPFile(byte[] fileContent) {
        return fileContent.length > 2 &&
                fileContent[0] == 'B' &&
                fileContent[1] == 'M';
    }
}
```

## 七、结果展示与分析

1. **图形用户界面** ：提供了直观的上传文件按钮和退出按钮，用户可以方便地选择文件进行上传操作，同时在更新时通过弹窗提示用户，增强了用户体验。"C:\Users\25637\Pictures\Screenshots\屏幕截图 2025-06-22 101905.png"
2. **选择上传文件** ：通过文件选择对话框，用户能够浏览并选择本地需要上传的文件，无论是普通文件还是 BMP 文件，均能进行相应的处理和上传操作。"C:\Users\25637\Pictures\Screenshots\屏幕截图 2025-06-22 102410.png"
3. **数据库纪录** ：文件上传成功后，文件的相关信息会被存储到 Sqlite 数据库中，方便对文件的管理和查询，能够准确记录文件名、文件大小、CRC32 校验值以及上传时间等信息。"C:\Users\25637\Pictures\Screenshots\屏幕截图 2025-06-22 101711.png"
4. **发送 BMP 文件隐写功能展示** ：在客户端上传 BMP 文件时，可以输入需要嵌入的隐写消息，服务端接收到文件后，能够正确嵌入消息并返回处理后的文件，客户端可以解析出隐写信息并展示给用户，验证了隐写功能的正确性。"C:\Users\25637\Pictures\Screenshots\屏幕截图 2025-06-22 102713.png"
5. **上传到 Github** ：将项目代码和文档上传到 Github，实现了代码的版本控制和远程存储，方便团队协作和代码备份，同时也有利于后续对项目的持续维护和更新。"C:\Users\25637\Pictures\Screenshots\屏幕截图 2025-06-22 110208.png"

## 八、课程设计的收获和体会

1. **知识技能的提升**
    * **文件处理与编码技术** ：掌握了文件的读取、写入操作以及 BASE64 编解码方法。能够将不同类型的文件进行编码转换，以便在网络中传输，并在接收端准确地还原文件内容，这对处理各种文件数据具有重要意义。
    * **数据库应用** ：学习了 Sqlite 数据库的基本操作，包括数据库的创建、表的建立、数据的插入和查询等。能够将文件的相关信息存储到数据库中，方便了文件的管理和检索，进一步拓展了对数据存储技术的应用范围。
    * **隐写技术探索** ：对 LSB 隐写技术有了深入的理解和实践。了解了信息隐藏的基本原理和方法，掌握了如何在 BMP 图像中嵌入和提取隐写信息，这为今后在信息安全、数字水印等领域的发展提供了有益的参考。

2. **问题解决能力的培养**
    * **调试与排错能力** ：在项目开发过程中，遇到了各种各样的问题，如网络连接异常、文件传输错误、多线程同步问题、隐写算法不准确等。通过使用调试工具、查看日志信息、分析程序逻辑等方式，逐步找到了问题的根源并加以解决，提高了调试和排错的能力，能够在面对复杂问题时保持冷静，有条不紊地进行分析和处理。
    * **需求分析与设计能力** ：通过对项目需求的仔细分析，规划了系统的整体架构和各个模块的功能。在设计过程中，不断地权衡各种方案的优缺点，选择了最适合项目需求的技术和算法。这锻炼了需求分析和系统设计的能力，使能够更加准确地把握项目目标，设计出合理、高效的软件系统。

3. **自主学习能力**
    * 为了完成项目中的各项任务，需要学习很多新的知识和技术，如 Java 的多线程、网络编程库的使用、Sqlite 数据库的集成、隐写技术的原理等。通过查阅文档、参考教程、研究示例代码等方式，自主学习并掌握了这些知识，培养了快速学习和自我提升的能力，能够更好地适应技术的不断更新和发展。

4. **对课程设计的思考与展望**
    * **反思** ：在项目的设计和实现过程中，还存在一些不足之处。例如，程序的健壮性有待提高，在处理一些异常情况时还不够完善；部分代码的结构可以进一步优化，提高可读性和可维护性；对于隐写技术的应用场景和安全性分析还不够深入等。这些都需要在今后的学习和实践中不断改进和完善。
    * **展望** ：通过本次课程设计，对嵌入式软件设计有了更全面的认识和实践体验。在未来的学习和工作中，希望能够将所学的知识应用到更复杂的项目中，进一步深入探索网络编程、信息安全、软件工程等领域的新技术和新方法。同时，不断加强自己的编程能力和创新思维，努力提高解决实际问题的能力，为成为一名优秀的软件工程师而努力奋斗。
