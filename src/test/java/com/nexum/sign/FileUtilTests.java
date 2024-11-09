package com.nexum.sign;

import static org.junit.jupiter.api.Assertions.*;

import com.nexum.sign.models.annexe.Annexe;
import com.nexum.sign.utils.FileUtil;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class FileUtilTests {

    @Test
    void attachAnnexe_shouldAttachAnnexesToPdf() throws IOException {
        String path = "D:\\proyectos\\nexum\\src\\test\\resources\\dumy.pdf";
        String base64Pdf = "";
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] fileBytes = new byte[fis.available()];
            fis.read(fileBytes);
            base64Pdf = Base64.getEncoder().encodeToString(fileBytes);
        }

        Annexe annexe = new Annexe("test.txt", Base64.getEncoder().encodeToString("dummy content".getBytes()));
        List<Annexe> annexes = Collections.singletonList(annexe);

        String result = FileUtil.attachAnnexe(base64Pdf, annexes);

        assertNotNull(result);
        assertTrue(result.length() > base64Pdf.length());
    }

    @Test
    void attachAnnexe_shouldHandleEmptyAnnexesList() throws IOException {
        String path = "D:\\proyectos\\nexum\\src\\test\\resources\\dumy.pdf";
        String base64Pdf = "";
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] fileBytes = new byte[fis.available()];
            fis.read(fileBytes);
            base64Pdf = Base64.getEncoder().encodeToString(fileBytes);
        }
        List<Annexe> annexes = Collections.emptyList();

        String result = FileUtil.attachAnnexe(base64Pdf, annexes);

        assertNotNull(result);
        assertTrue(result.length() == base64Pdf.length());
    }

    @Test
    void attachAnnexe_shouldHandleInvalidBase64Pdf() {
        String invalidBase64Pdf = "invalid base64";
        Annexe annexe = new Annexe("dummy name", Base64.getEncoder().encodeToString("dummy content".getBytes()));
        List<Annexe> annexes = Collections.singletonList(annexe);

        assertThrows(IllegalArgumentException.class, () -> {
            FileUtil.attachAnnexe(invalidBase64Pdf, annexes);
        });
    }

    @Test
    void attachAnnexe_shouldHandleInvalidBase64Annexe() throws IOException {
        String path = "D:\\proyectos\\nexum\\src\\test\\resources\\dumy.pdf";
        String base64Pdf = "";
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] fileBytes = new byte[fis.available()];
            fis.read(fileBytes);
            base64Pdf = Base64.getEncoder().encodeToString(fileBytes);
        }

        Annexe annexe = new Annexe("dummy name", "invalid base64");
        List<Annexe> annexes = Collections.singletonList(annexe);

        String finalBase64Pdf = base64Pdf;
        assertThrows(IllegalArgumentException.class, () -> {
            FileUtil.attachAnnexe(finalBase64Pdf, annexes);
        });
    }
}