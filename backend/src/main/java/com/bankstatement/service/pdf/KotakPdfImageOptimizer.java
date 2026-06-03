package com.bankstatement.service.pdf;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/** Downscales Kotak PNG assets before iText embed to reduce PDF size and FinBox scan heuristics. */
final class KotakPdfImageOptimizer {

    private KotakPdfImageOptimizer() {}

    static byte[] resizeToMaxWidth(byte[] pngBytes, int maxWidth) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (source == null) {
            throw new IllegalStateException("Unable to read PNG bytes");
        }
        if (source.getWidth() <= maxWidth) {
            return pngBytes;
        }
        int newHeight = (int) Math.round(source.getHeight() * (maxWidth / (double) source.getWidth()));
        return toPng(scale(source, maxWidth, newHeight));
    }

    static byte[] resizeToExact(byte[] pngBytes, int width, int height) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (source == null) {
            throw new IllegalStateException("Unable to read PNG bytes");
        }
        if (source.getWidth() == width && source.getHeight() == height) {
            return pngBytes;
        }
        return toPng(scale(source, width, height));
    }

    private static BufferedImage scale(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private static byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Embeds as {@code /Filter/DCTDecode} in PDF (original Kotak reference uses JPEG XObjects).
     */
    static byte[] toJpeg(byte[] imageBytes, float quality) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (source == null) {
            throw new IllegalStateException("Unable to read image bytes");
        }
        return toJpeg(flattenAlpha(source), quality);
    }

    static byte[] toJpeg(BufferedImage image, float quality) throws IOException {
        BufferedImage rgb = image.getType() == BufferedImage.TYPE_INT_RGB
                ? image
                : flattenAlpha(image);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static BufferedImage flattenAlpha(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
    }
}
