package com.bankstatement.service.pdf;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Reuses the same image XObject for repeated logos across a PDF. */
final class BankPdfImageCache {

    private final Map<String, ImageData> cache = new HashMap<>();

    ImageData get(String classpath) {
        return cache.computeIfAbsent(classpath, this::load);
    }

    Image scaledImage(String classpath, float height) {
        Image img = new Image(get(classpath));
        img.setHeight(height);
        img.setAutoScale(true);
        return img;
    }

    Image scaledImage(String classpath, float height, boolean autoScaleWidth) {
        Image img = new Image(get(classpath));
        img.setHeight(height);
        if (autoScaleWidth) {
            img.setAutoScaleWidth(true);
        } else {
            img.setAutoScale(true);
        }
        return img;
    }

    private ImageData load(String classpath) {
        try (InputStream in = new ClassPathResource(classpath).getInputStream()) {
            return ImageDataFactory.create(in.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load image: " + classpath, e);
        }
    }
}
