package at.ac.oeaw.helpers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public final class FileReader {
    public static String readFile(String filePath) throws IOException {
        InputStream is = new FileInputStream(filePath);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));

        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();
        while (line != null) {
            sb.append(line).append("\n");
            line = buf.readLine();
        }
        String file = sb.toString();
        return file;
    }

    public static BufferedImage readImage(String filePath) throws IOException {
        return ImageIO.read(new File(filePath));
    }
}
