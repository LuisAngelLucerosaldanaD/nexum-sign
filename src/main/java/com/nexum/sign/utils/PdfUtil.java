package com.nexum.sign.utils;

import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.nexum.sign.models.signer.AcrossField;
import com.nexum.sign.models.signer.Signer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class PdfUtil {
    public static String SetAcrossFields(String pdf, List<Signer> signers) throws IOException {
        byte[] pdfBytes = Base64.getDecoder().decode(pdf);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(inputStream), new PdfWriter(output));

        for (Signer signer : signers) {
            for (AcrossField field : signer.across_fields) {
                PdfPage page = pdfDoc.getPage(field.page);
                PdfCanvas canvas = new PdfCanvas(page);
                canvas.beginText()
                        .setFontAndSize(PdfFontFactory.createFont(), 12)
                        .moveText(field.x, field.y)
                        .showText(field.text)
                        .endText();
            }
        }
        pdfDoc.close();

        String result = Base64.getEncoder().encodeToString(output.toByteArray());
        output.close();
        inputStream.close();

        return result;
    }
}
