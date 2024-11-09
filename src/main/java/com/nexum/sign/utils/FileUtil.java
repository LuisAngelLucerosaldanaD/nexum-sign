package com.nexum.sign.utils;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import com.nexum.sign.models.annexe.Annexe;

import java.io.*;
import java.util.Base64;
import java.util.List;

public class FileUtil {
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
}
