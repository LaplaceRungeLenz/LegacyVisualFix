import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Reproducible, original procedural assets. Run with Java 25: java tools/GenerateThemeAssets.java */
public class GenerateThemeAssets {
    public static void main(String[] args) throws Exception {
        File directory = new File("src/main/resources/assets/modernnh/textures/gui");
        directory.mkdirs();
        BufferedImage bg = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(12, 22, 31), 1600, 900, new Color(24, 45, 53)));
        g.fillRect(0, 0, 1600, 900);
        g.setColor(new Color(72, 125, 133, 22));
        g.setStroke(new BasicStroke(1));
        for (int x = -900; x < 2500; x += 100) g.drawLine(x, 0, x + 900, 900);
        for (int x = 0; x < 2500; x += 100) g.drawLine(x, 0, x - 900, 900);
        g.setColor(new Color(93, 204, 170, 65));
        g.setStroke(new BasicStroke(2));
        g.drawLine(0, 270, 570, 270);
        g.drawLine(570, 270, 640, 200);
        g.drawLine(640, 200, 1600, 200);
        g.dispose();
        ImageIO.write(bg, "png", new File(directory, "background.png"));
        BufferedImage white = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        g = white.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 8, 8);
        g.dispose();
        ImageIO.write(white, "png", new File(directory, "bar_track.png"));
        ImageIO.write(white, "png", new File(directory, "bar_fill.png"));
    }
}
