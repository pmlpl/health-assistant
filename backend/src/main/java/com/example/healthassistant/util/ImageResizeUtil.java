package com.example.healthassistant.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 上传识图前缩小分辨率，降低视觉模型按图计费的 Token 消耗。
 */
public final class ImageResizeUtil {

    private ImageResizeUtil() {
    }

    /**
     * 将最长边限制在 maxSide 像素内，保持比例；失败则返回原图。
     */
    public static byte[] resizeForVision(byte[] input, String formatName, int maxSide) {
        if (input == null || input.length == 0 || maxSide <= 0) {
            return input;
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(input));
            if (src == null) {
                return input;
            }
            int w = src.getWidth();
            int h = src.getHeight();
            int longest = Math.max(w, h);
            if (longest <= maxSide) {
                return input;
            }
            double scale = (double) maxSide / longest;
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
            BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.drawImage(scaled, 0, 0, null);
            g.dispose();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String fmt = normalizeFormat(formatName);
            ImageIO.write(out, fmt, bos);
            return bos.toByteArray();
        } catch (IOException e) {
            return input;
        }
    }

    private static String normalizeFormat(String formatName) {
        if (formatName == null) {
            return "jpg";
        }
        return switch (formatName.toLowerCase()) {
            case "png" -> "png";
            case "gif" -> "gif";
            case "webp" -> "webp";
            default -> "jpg";
        };
    }
}
