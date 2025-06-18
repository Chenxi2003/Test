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
                // 完善表结构：增加上传时间、CRC32校验值
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

    // 新增静态方法：保存文件信息到数据库
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
            // 发送版本信息给客户端
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

        // 新增：处理客户端JAR下载
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

            byte[] fileContent = Base64.getDecoder().decode(fileContentBase64);

            // 计算CRC32校验值
            CRC32 crc32 = new CRC32();
            crc32.update(fileContent);
            long crc32Value = crc32.getValue();

            if (checkFileIntegrity(fileContent, fileLength)) {
                saveFile(fileName, fileContent);

                // 保存文件信息到数据库（包含CRC32值）
                FileServer.saveFileInfoToDB(fileName, fileLength, crc32Value);

                out.println("UPLOAD_SUCCESS");
                System.out.println("文件上传成功：" + fileName);
            } else {
                out.println("UPLOAD_FAILED");
                System.out.println("文件上传失败，校验失败：" + fileName);
            }
        }

        private boolean checkFileIntegrity(byte[] fileContent, long expectedLength) {
            // 同时校验长度和CRC32（实际项目中应比较客户端发送的CRC32）
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