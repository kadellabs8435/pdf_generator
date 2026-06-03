package com.bankstatement.service.pdf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Trims white margins from the Kotak promotional banner so it aligns with summary section bars. */
final class KotakAdBannerImage {

    private KotakAdBannerImage() {}

    static byte[] loadTrimmedPng(InputStream in) throws Exception {
        BufferedImage source = ImageIO.read(in);
        if (source == null) {
            throw new IllegalStateException("Unable to read AdKotak banner");
        }
        BufferedImage trimmed = trimWhitespace(source);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(trimmed, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage trimWhitespace(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int minX = width;
        int minY = height;
        int maxX = 0;
        int maxY = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isWhitespace(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }
        return image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static boolean isWhitespace(int rgb) {
        int alpha = (rgb >> 24) & 0xFF;
        if (alpha < 16) {
            return true;
        }
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        // Trim white and light-gray page margins (e.g. rgb 204 and 241 in source PNG).
        return red >= 200 && green >= 200 && blue >= 200;
    }
}
