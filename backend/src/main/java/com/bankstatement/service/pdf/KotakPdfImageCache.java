package com.bankstatement.service.pdf;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Kotak-only image cache with pre-embed PNG optimization (visual size unchanged in layout). */
final class KotakPdfImageCache {

    static final String IMAGE_LEFT = "pdf/kotak/image-left.png";
    static final String KOTAK_LOGO = "pdf/kotak/kotak-logo.png";

    private static final int MAX_WIDTH_IMAGE_LEFT = 220;
    private static final int MAX_WIDTH_KOTAK_LOGO = 420;

    private final Map<String, ImageData> cache = new HashMap<>();

    ImageData get(String classpath) {
        return cache.computeIfAbsent(classpath, this::loadOptimized);
    }

    Image scaledImage(String classpath, float height) {
        Image img = new Image(get(classpath));
        img.setHeight(height);
        img.setAutoScale(true);
        return img;
    }

    private ImageData loadOptimized(String classpath) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            byte[] raw = in.readAllBytes();
            byte[] optimized = switch (classpath) {
                case IMAGE_LEFT -> KotakPdfImageOptimizer.toJpeg(
                        KotakPdfImageOptimizer.resizeToMaxWidth(raw, MAX_WIDTH_IMAGE_LEFT), 0.92f);
                case KOTAK_LOGO -> KotakPdfImageOptimizer.toJpeg(
                        KotakPdfImageOptimizer.resizeToMaxWidth(raw, MAX_WIDTH_KOTAK_LOGO), 0.92f);
                default -> KotakPdfImageOptimizer.toJpeg(raw, 0.92f);
            };
            return ImageDataFactory.create(optimized);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Kotak image: " + classpath, e);
        }
    }
}
