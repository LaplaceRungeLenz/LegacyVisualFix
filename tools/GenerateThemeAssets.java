import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Original, reproducible gradient and pixel bevels; no raster concept artwork is shipped. */
public class GenerateThemeAssets {
    public static void main(String[] args) throws Exception {
        File directory = new File("src/main/resources/assets/modernnh/textures/gui");
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
        BufferedImage track = new BufferedImage(240, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = track.createGraphics();
        g.setColor(new Color(0x101923)); g.fillRect(0, 0, 240, 10);
        g.setColor(new Color(0x71808A)); g.fillRect(1, 0, 238, 1);
        g.setColor(new Color(0x46535F)); g.fillRect(0, 1, 1, 8); g.fillRect(239, 1, 1, 8);
        g.setColor(new Color(0x33414F)); g.fillRect(1, 9, 238, 1);
        g.setColor(new Color(0x263441)); g.fillRect(2, 2, 236, 6);
        g.dispose();
        ImageIO.write(track, "png", new File(directory, "bar_track.png"));
        BufferedImage fill = new BufferedImage(240, 6, BufferedImage.TYPE_INT_ARGB);
        int[] rows = {0xFFF0B4, 0xEACB79, 0xDDB85D, 0xDDB85D, 0xCAA14C, 0xA77B32};
        for (int y = 0; y < 6; y++) for (int x = 0; x < 240; x++) fill.setRGB(x, y, 0xFF000000 | rows[y]);
        ImageIO.write(fill, "png", new File(directory, "bar_fill.png"));
    }
}
