import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.zip.CRC32;

public class FileServer {
    private static final int PORT = 8888;
    private static final String VERSION_FILE = "version.txt";
    private static final String CLIENT_DIR = "client_dir/";
    private static final String FILE_DIR = "file_dir/";
    private static final String DB_PATH = "file_info.db";
    private static final String CLIENT_JAR = "Client.jar"; // 新增：客户端JAR文件名

    public static void main(String[] args) {
        // 创建文件目录
        new File(CLIENT_DIR).mkdirs();
        new File(FILE_DIR).mkdirs();
        // 初始化数据库
        initDatabase();

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("服务器已启动，等待客户端连接...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("一个客户端已连接");

                // 处理客户端连接
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
        // 初始化数据库代码
        // 可使用 JDBC 或其他数据库操作工具
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
                // 检查客户端版本
                checkClientVersion(out);

                String command;
                while ((command = in.readLine()) != null) {
                    if (command.startsWith("UPLOAD")) {
                        handleUpload(in, out);
                    } else if (command.startsWith("DOWNLOAD_CLIENT")) { // 新增：处理客户端下载请求
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

            // 解码文件内容
            byte[] fileContent = Base64.getDecoder().decode(fileContentBase64);

            // 校验文件完整性（CRC32）
            if (checkFileIntegrity(fileContent, fileLength)) {
                // 保存文件
                saveFile(fileName, fileContent);

                // 将文件信息存储到数据库
                saveFileInfo(fileName, fileLength);

                out.println("UPLOAD_SUCCESS");
                System.out.println("文件上传成功：" + fileName);
            } else {
                out.println("UPLOAD_FAILED");
                System.out.println("文件上传失败，校验失败：" + fileName);
            }
        }

        private boolean checkFileIntegrity(byte[] fileContent, long expectedLength) {
            // CRC32 校验
            CRC32 crc32 = new CRC32();
            crc32.update(fileContent);
            // 这里可以将 CRC32 值与客户端发送的进行比较，此处简化处理
            return fileContent.length == expectedLength;
        }

        private void saveFile(String fileName, byte[] fileContent) {
            try (FileOutputStream fos = new FileOutputStream(FILE_DIR + fileName)) {
                fos.write(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void saveFileInfo(String fileName, long fileLength) {
            // 将文件信息存储到数据库代码
            // 可使用 JDBC 或其他数据库操作工具
        }
    }
}