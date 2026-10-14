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
            graphics.setFont(new Font("Arial", Font.BOLD, 12));
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


}
