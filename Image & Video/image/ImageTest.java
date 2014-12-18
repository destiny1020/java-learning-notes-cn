package image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.Test;

public class ImageTest {

    @Test
    public void testGenerateAPI() throws IOException {
        ImageStoryboardGenerator.generate(new File("D:/Images").listFiles(),
                "Sample header", 100, 100, "output.jpg");
    }

    @Test
    public void testCombineImages() throws IOException {
        int imageWidth = 100;
        int imageHeight = 100;

        File[] images = Paths.get("D:/Images").toFile().listFiles();

        int column = 5;
        int row = (images.length / column)
                + (images.length % column == 0 ? 0 : 1);

        System.out.println(String.format("Row: %d, Column: %d", row, column));

        // +1 means to leave a header area for description
        BufferedImage result = new BufferedImage(imageWidth * column,
                imageHeight * (row + 1), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();

        drawHeader(g, "Sample Header", imageWidth * column, imageHeight);

        int x = 0, y = imageHeight;
        for (File image : images) {
            BufferedImage bi = ImageIO.read(image);
            watermark(bi, image.getName());
            g.drawImage(bi, x, y, null);
            x += bi.getWidth();
            if (x >= result.getWidth()) {
                x = 0;
                y += bi.getHeight();
            }
        }

        ImageIO.write(result, "png", new File("result.png"));
    }

    // draw a header with white foreground color and black background color
    private void drawHeader(Graphics2D g, String header, int width, int height) {
        // draw the background
        Rectangle2D headerBounds = new Rectangle2D.Float(0, 0, width, height);

        g.setColor(Color.BLACK);
        g.fill(headerBounds);
        g.setColor(Color.WHITE);

        // header text bounds
        Rectangle2D bounds = g.getFont().getStringBounds(header,
                g.getFontRenderContext());

        System.out.println("Text: " + bounds.getWidth() + ":"
                + bounds.getHeight());

        g.drawString(header, (width - (int) bounds.getWidth()) / 2,
                (height - (int) (bounds.getHeight())) / 2);
    }

    private void watermark(BufferedImage image, String watermark) {
        Graphics2D g = image.createGraphics();
        Rectangle2D bounds = g.getFont().getStringBounds(watermark,
                g.getFontRenderContext());

        double insetX = bounds.getWidth();
        double insetY = bounds.getHeight();
        g.translate(0, image.getHeight() - insetY);
        g.setColor(Color.WHITE);
        g.drawString(watermark, 0, 0);
    }

}
