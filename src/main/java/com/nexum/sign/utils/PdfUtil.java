package com.nexum.sign.utils;

import com.nexum.sign.models.PdfImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.Loader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PdfUtil {
    public static List<PdfImage> getPagesToImg(byte[] pdfBytes) throws IOException {
        PDDocument document = Loader.loadPDF(pdfBytes);
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        List<PdfImage> images = new ArrayList<>();
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            PDPage pdPage = document.getPage(page);
            BufferedImage image = pdfRenderer.renderImageWithDPI(page, 150);
            float scaleY = pdPage.getCropBox().getHeight() / image.getHeight();
            float scaleX = pdPage.getCropBox().getWidth() / image.getWidth();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            String imageBase64 = Base64.getEncoder().encodeToString(out.toByteArray());
            String name = scaleX + "x" + scaleY + (page + 1) + "-.png";
            images.add(new PdfImage(FileUtil.PNG_MIME + imageBase64, page + 1, name, image.getWidth(), image.getHeight()));
        }

        return images;
    }
}
