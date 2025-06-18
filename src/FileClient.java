import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.Scanner;

public class FileClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final String VERSION_FILE = "version.txt";
    private static final String SERVER_VERSION_FILE = "server_version.txt"; // 新增：服务器版本文件
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
            // 连接服务器
            connectToServer();

            // 1. 检查服务器版本更新
            checkServerVersionUpdate();

            // 2. 检查客户端自身更新
            checkVersionUpdate();

            // 启动多线程
            Thread interactionThread = new Thread(new InteractionTask());
            interactionThread.start();

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

    // 新增：检查服务器版本更新
    private void checkServerVersionUpdate() throws IOException {
        // 接收服务器版本
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

            // 替换旧客户端
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
        // 发送版本信息给服务器
        String version = getVersion();
        out.println("CLIENT_VERSION " + version);

        // 接收服务器响应
        String response = in.readLine();
        if (response != null && response.startsWith("NEW_VERSION_AVAILABLE")) {
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