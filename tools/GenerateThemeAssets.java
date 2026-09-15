import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Original, reproducible gradient and pixel bevels; no raster concept artwork is shipped. */
public class GenerateThemeAssets {
    public static void main(String[] args) throws Exception {
        File directory = new File("src/main/resources/assets/legacyvisualfix/textures/gui");
        directory.mkdirs();
        BufferedImage bg = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 900; y++) {
            for (int x = 0; x < 1600; x++) {
                double dx = (x - 800.0) / 690;
                double dy = (y - 360.0) / 430;
                double glow = Math.exp(-(dx * dx + dy * dy) * 1.6);
                int r = 11 + (int) Math.round(10 * glow);
                int g = 21 + (int) Math.round(34 * glow);
                int b = 33 + (int) Math.round(47 * glow);
                bg.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        ImageIO.write(bg, "png", new File(directory, "background.png"));
        BufferedImage track = new BufferedImage(8, 6, BufferedImage.TYPE_INT_ARGB);
        BufferedImage fill = new BufferedImage(8, 6, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 6; y++) for (int x = 0; x < 8; x++) {
            track.setRGB(x, y, 0xFF293B4B);
            fill.setRGB(x, y, 0xFFE1BC62);
        }
        ImageIO.write(track, "png", new File(directory, "bar_track.png"));
        ImageIO.write(fill, "png", new File(directory, "bar_fill.png"));
    }
}
