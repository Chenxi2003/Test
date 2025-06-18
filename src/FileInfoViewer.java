import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class FileInfoViewer {
    // 数据库路径
    private static final String DB_PATH = "D:/IDEA project/JavaAIproject/keshe/file_info.db"; // 改成你实际的路径

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM files")) {

            System.out.println("数据库文件: " + DB_PATH);
            System.out.println("文件信息表 contents:");
            System.out.println("--------------------------------------------------");
            System.out.printf("%-5s %-20s %-10s %-15s %-20s\n", "ID", "文件名", "长度", "CRC32", "上传时间");
            System.out.println("--------------------------------------------------");

            while (rs.next()) {
                int id = rs.getInt("id");
                String fileName = rs.getString("filename");
                long fileLength = rs.getLong("filelength");
                long crc32 = rs.getLong("crc32");
                String uploadTime = rs.getString("upload_time");

                System.out.printf("%-5d %-20s %-10d %-15d %-20s\n", 
                    id, fileName, fileLength, crc32, uploadTime);
            }

        } catch (Exception e) {
            System.out.println("数据库访问时发生错误: " + e.getMessage());
        }
    }
}
