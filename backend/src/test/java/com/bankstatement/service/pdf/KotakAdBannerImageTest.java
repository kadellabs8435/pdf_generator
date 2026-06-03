package com.bankstatement.service.pdf;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

class KotakAdBannerImageTest {

    @Test
    void trimsWhiteMarginsFromBanner() throws Exception {
        try (var in = new ClassPathResource("pdf/kotak/AdKotak.png").getInputStream()) {
            byte[] trimmed = KotakAdBannerImage.loadTrimmedPng(in);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(trimmed));
            assertNotNull(image);
            assertTrue(image.getWidth() > 0);
            assertTrue(image.getHeight() > 0);
            assertTrue(image.getWidth() < 1200, "Gray/white margins should be trimmed, width was " + image.getWidth());
            int leftPixel = image.getRGB(0, image.getHeight() / 2);
            int red = (leftPixel >> 16) & 0xFF;
            assertTrue(red < 100, "Left edge should be banner content, not margin gray");
        }
    }
}
