import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ExtractHiddenText {

    public static void main(String[] args) {
        try {
            // 修改为你自己的图片路径
            BufferedImage image = ImageIO.read(new File("D:\\IDEA project\\JavaAIproject\\keshe\file_dir"));

            if (image == null) {
                System.out.println("图片加载失败，请检查路径或格式！");
                return;
            }

            String hiddenText = extractText(image);
            System.out.println("提取到的隐藏文字: " + hiddenText);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String extractText(BufferedImage image) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        // 创建相同大小的位置数组
        int[] positions = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            positions[i] = i;
        }

        // 使用相同的随机算法打乱位置（与嵌入时一致）
        shuffleArray(positions);

        // 提取前32位以获得文本长度
        int textLength = 0;
        int bitIndex = 0;

        for (int i = 0; i < 32; i++) {
            int pixelIndex = positions[bitIndex];
            int x = pixelIndex % width;
            int y = pixelIndex / height;

            int rgb = image.getRGB(x, y);
            int bit = (rgb >> ((bitIndex % 3))) & 1; // 根据通道提取位
            textLength = (textLength << 1) | bit;
            bitIndex++;
        }

        if (textLength <= 0) {
            throw new IOException("未找到有效的隐藏文本");
        }

        // 提取实际文本内容
        byte[] textBytes = new byte[textLength];
        for (int byteIndex = 0; byteIndex < textLength; byteIndex++) {
            byte currentByte = 0;
            for (int bitPosition = 0; bitPosition < 8; bitPosition++) {
                int pixelIndex = positions[bitIndex];
                int x = pixelIndex % width;
                int y = pixelIndex / height;

                int rgb = image.getRGB(x, y);
                int bit = (rgb >> ((bitIndex % 3))) & 1; // 根据通道提取位
                currentByte = (byte) ((currentByte << 1) | bit);
                bitIndex++;
            }
            textBytes[byteIndex] = currentByte;
        }

        return new String(textBytes, "UTF-8");
    }

    // 随机打乱数组（Fisher-Yates算法，与嵌入时一致）
    private static void shuffleArray(int[] array) {
        java.util.Random rnd = new java.util.Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
}
