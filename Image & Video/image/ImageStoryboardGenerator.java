package image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageStoryboardGenerator {

    /**
     * Create an image storyboard based on images, with a header on top of it.
     * The images should have same dimensions(unitWidth and unitHeight).
     * 
     * @param images
     * @param header
     * @param unitWidth
     * @param unitHeight
     * @throws IOException 
     */
    public static void generate(File[] images, String header, int unitWidth,
            int unitHeight, String outputPath) throws IOException {
        int column = 5;
        int row = (images.length / column)
                + (images.length % column == 0 ? 0 : 1);

        boolean headerNeeded = header != null && !header.isEmpty();
        String imageType = outputPath.split("\\.")[1];

        if (headerNeeded) {
            // additional row for the header
            row += 1;
        }

        int imageColorMode;
        if (imageType.equalsIgnoreCase("png")) {
            imageColorMode = BufferedImage.TYPE_INT_ARGB;
        } else {
            imageColorMode = BufferedImage.TYPE_INT_BGR;
        }

        BufferedImage output = new BufferedImage(unitWidth * column, unitHeight
                * row, imageColorMode);
        Graphics2D g = output.createGraphics();

        // draw the header
        if (headerNeeded) {
            drawHeader(g, header, unitWidth * column, unitHeight);
        }

        // draw each image
        int x = 0, y = headerNeeded ? unitHeight : 0;
        int outputWidth = output.getWidth();
        for (File image : images) {
            BufferedImage bi = ImageIO.read(image);
            putLabel(bi, image.getName());
            g.drawImage(bi, x, y, null);
            x += unitWidth;
            if (x >= outputWidth) {
                x = 0;
                y += unitHeight;
            }
        }

        ImageIO.write(output, imageType, new File(outputPath));
    }

    // draw a header with white foreground color and black background color
    private static void drawHeader(Graphics2D g, String header, int width,
            int height) {
        // draw the background
        Rectangle2D headerBounds = new Rectangle2D.Float(0, 0, width, height);

        g.setColor(Color.BLACK);
        g.fill(headerBounds);
        g.setColor(Color.WHITE);

        // header text bounds
        Rectangle2D bounds = g.getFont().getStringBounds(header,
                g.getFontRenderContext());

        g.drawString(header, (width - (int) bounds.getWidth()) / 2,
                (height - (int) (bounds.getHeight())) / 2);
    }

    private static void putLabel(BufferedImage image, String label) {
        Graphics2D g = image.createGraphics();
        Rectangle2D bounds = g.getFont().getStringBounds(label,
                g.getFontRenderContext());

        double insetX = (image.getWidth() - bounds.getWidth()) / 2;
        double insetY = bounds.getHeight();
        g.translate(insetX, image.getHeight() - insetY);
        g.setColor(Color.WHITE);
        g.drawString(label, 0, 0);
    }

}
