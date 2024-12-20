package com.nexum.sign.utils;

import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import com.nexum.sign.models.annexe.Annexe;
import com.nexum.sign.models.signer.AcrossField;
import com.nexum.sign.models.signer.Signer;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

public class FileUtil {

    public static String PNG_MIME = "data:image/png;base64,";
    public static void base64ToStore(String file, String path) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(file);
        File fileByt = new File(path);
        FileOutputStream fos = new FileOutputStream(fileByt);
        fos.write(decodedBytes);
        fos.flush();
        fos.close();
    }

    public static void deleteFile(String path) throws ErrorUtil {
        File file = new File(path);
        if (!file.exists()) return;

        boolean isDeleted = file.delete();
        if (isDeleted) return;

        throw new ErrorUtil("No se pudo eliminar el archivo");
    }

    public static String getBase64ToPath(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] fileBytes = new byte[fis.available()];
            fis.read(fileBytes);
            return Base64.getEncoder().encodeToString(fileBytes);
        }
    }

    public static String attachAnnexe(String base64Pdf, List<Annexe> annexes) throws IOException {
        if (annexes.isEmpty()) return base64Pdf;

        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)), new PdfWriter(outputStream));
        for (Annexe annexe : annexes) {
            byte[] decodedBytes = Base64.getDecoder().decode(annexe.encode);
            PdfFileSpec spec = PdfFileSpec.createEmbeddedFileSpec(pdfDoc, decodedBytes,
                    annexe.original, annexe.original, null, null, null);
            pdfDoc.addFileAttachment(annexe.original, spec);
        }

        pdfDoc.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static String configureDocument(String base64Pdf) throws IOException {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)), new PdfWriter(outputStream));
        pdfDoc.getDocumentInfo().setCreator("NexumSign");
        pdfDoc.getDocumentInfo().setProducer("NexumSign");
        pdfDoc.close();
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static String getFileHash(String file) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = Base64.getDecoder().decode(file);
        return getHash(fileBytes);
    }

    public static String getHash(byte[] file) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(file);
        return new BigInteger(1, hash).toString(16);
    }

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
