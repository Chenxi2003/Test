import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Scanner;
import java.util.zip.CRC32;

public class FileClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final String VERSION_FILE = "version.txt";
    private static final String CLIENT_JAR = "Client.jar";

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        new FileClient().start();
    }

    public void start() {
        try {
            // 启动多线程
            Thread interactionThread = new Thread(new InteractionTask());
            interactionThread.start();

            // 连接服务器
            connectToServer();

            // 检查版本更新
            checkVersionUpdate();

            // 等待用户交互线程结束
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

    private void checkVersionUpdate() throws IOException {
        // 发送版本信息给服务器
        String version = getVersion();
        out.println("CLIENT_VERSION " + version);

        // 接收服务器响应
        String response = in.readLine();
        if (response.startsWith("NEW_VERSION_AVAILABLE")) {
            String newVersion = response.split(" ")[1];
            downloadNewVersion(newVersion);
            System.out.println("新的客户端版本可用，已下载并替换旧版本");
            // 退出当前客户端
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
        // 下载新版本客户端代码
        // 可使用 HTTP 请求或其他方式下载新版本文件
        // 此处简化处理，实际项目中需实现具体的下载逻辑
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
            // 读取文件内容并进行 BASE64 编码
            byte[] fileContent = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(fileContent);
            }
            String fileContentBase64 = Base64.getEncoder().encodeToString(fileContent);

            // 发送上传请求
            out.println("UPLOAD");
            out.println(fileName);
            out.println(file.length());
            out.println(fileContentBase64);

            // 接收服务器响应
            String response = in.readLine();
            if ("UPLOAD_SUCCESS".equals(response)) {
                System.out.println("文件上传成功：" + fileName);
            } else {
                System.out.println("文件上传失败：" + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
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
    }
}
