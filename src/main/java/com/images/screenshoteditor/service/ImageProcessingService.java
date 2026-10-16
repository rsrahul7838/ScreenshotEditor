package com.images.screenshoteditor.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Service
public class ImageProcessingService {



    public BufferedImage addAnnotation(
            BufferedImage original,
            int x,
            int y,
            int width,
            int height) {

        BufferedImage copy = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = copy.createGraphics();

        try {
            // Copy the original image
            graphics.drawImage(original, 0, 0, null);

            // Highlight the selected region
            graphics.setColor(Color.RED);
            graphics.setStroke(new java.awt.BasicStroke(3));

            graphics.drawRect(
                    x,
                    y,
                    width - 1,
                    height - 1
            );

            // Label the selection for testing
            graphics.setColor(Color.YELLOW);
            graphics.fillRect(x, Math.max(0, y - 24), 150, 22);

            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            graphics.drawString(
                    "SELECTED REGION",
                    x + 5,
                    Math.max(15, y - 8)
            );

        } finally {
            graphics.dispose();
        }

        return copy;
    }

    public BufferedImage createMockupCopy(BufferedImage original) {
        BufferedImage copy = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = copy.createGraphics();

        try {
            graphics.drawImage(original, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return copy;
    }


    public BufferedImage renderTestText(
            BufferedImage original,
            int x,
            int y,
            int width,
            int height,
            String replacementText) {

        if (original == null || replacementText == null
                || replacementText.isBlank()) {
            throw new IllegalArgumentException(
                    "Image and replacement text are required.");
        }

        if (x < 0 || y < 0 || width <= 0 || height <= 0
                || x >= original.getWidth()
                || y >= original.getHeight()
                || width > original.getWidth() - x
                || height > original.getHeight() - y) {
            throw new IllegalArgumentException(
                    "The selected region is outside the image.");
        }

        BufferedImage copy = createMockupCopy(original);
        Graphics2D graphics = copy.createGraphics();

        try {
            // This method is intended for the supplied synthetic
            // test image, whose text is on a white background.
            graphics.setColor(new Color(218, 233, 242));
            graphics.fillRect(x, y, width, height);

            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Segoe UI", Font.PLAIN, 12));


            java.awt.FontMetrics metrics = graphics.getFontMetrics();
            int textY = y + (height - metrics.getHeight()) / 2
                    + metrics.getAscent();


            int textX = x + width - metrics.stringWidth(replacementText) - 2;


            graphics.drawString(replacementText, textX, textY);
        } finally {
            graphics.dispose();
        }

        return copy;
    }

    public BufferedImage renderTwoTestValues(
            BufferedImage original,
            int dateX, int dateY, int dateWidth, int dateHeight,
            String newDate,
            int timeX, int timeY, int timeWidth, int timeHeight,
            String newTime) {
        if (newDate != null && !newDate.isBlank()
                && newTime != null && !newTime.isBlank()
                && regionsOverlap(
                dateX, dateY, dateWidth, dateHeight,
                timeX, timeY, timeWidth, timeHeight)) {

            throw new IllegalArgumentException(
                    "Date and time regions overlap. Please select separate regions.");
        }

        BufferedImage output = createMockupCopy(original);

        if (newDate != null && !newDate.isBlank()) {
            output = renderTestText(
                    output,
                    dateX, dateY, dateWidth, dateHeight,
                    newDate
            );
        }

        if (newTime != null && !newTime.isBlank()) {
            output = renderTestText(
                    output,
                    timeX, timeY, timeWidth, timeHeight,
                    newTime
            );
        }

        return output;
    }

    private boolean regionsOverlap(
            int x1, int y1, int w1, int h1,
            int x2, int y2, int w2, int h2) {

        return x1 < x2 + w2
                && x1 + w1 > x2
                && y1 < y2 + h2
                && y1 + h1 > y2;
    }




}
