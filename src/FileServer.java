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

                // 保存文件信息到数据库
                FileServer.saveFileInfoToDB(fileName, fileLength, finalCrc32);

                out.println("UPLOAD_SUCCESS");
                System.out.println("文件上传成功：" + fileName);
            } else {
                out.println("UPLOAD_FAILED");
                System.out.println("文件上传失败，校验失败：" + fileName);
            }
            // 嵌入消息后提取隐写信息
            String extractedMessage = null;
            if (fileName.toLowerCase().endsWith(".bmp") && !messageToEmbed.isEmpty()) {
                extractedMessage = LSBSteganographyAnalyzer.extractLSBMessage(finalFileContent);
            }

            if (checkFileIntegrity(finalFileContent, fileLength)) {
                saveFile(fileName, finalFileContent);
                FileServer.saveFileInfoToDB(fileName, fileLength, finalCrc32);

                // 返回上传成功和隐写信息
                out.println("UPLOAD_SUCCESS");
                out.println(extractedMessage != null ? extractedMessage : "NO_STEGO_DETECTED"); // 新增行
                System.out.println("文件上传成功：" + fileName);
            } else {
                out.println("UPLOAD_FAILED");
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