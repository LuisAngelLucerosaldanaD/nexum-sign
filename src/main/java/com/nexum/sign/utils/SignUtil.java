package com.nexum.sign.utils;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import com.nexum.sign.models.signer.Signer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class SignUtil {
    public static String Sign(String pdf, Certificate[] chain,
                              PrivateKey pk, String digestAlgorithm,
                              PdfSigner.CryptoStandard subFilter, List<Signer> signers)
            throws GeneralSecurityException, IOException {

        byte[] pdfBytes = Base64.getDecoder().decode(pdf);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));

        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        IExternalDigest digest = new BouncyCastleDigest();
        IExternalSignature pks = new PrivateKeySignature(pk, digestAlgorithm, BouncyCastleProvider.PROVIDER_NAME);

        for (Signer signer : signers) {
            PdfSigner pdfSigner = GetPdfSigner(signer, reader, output);
            pdfSigner.signDetached(digest, pks, chain, null, null, null, 0, subFilter);
        }

        String result = Base64.getEncoder().encodeToString(output.toByteArray());
        output.close();
        reader.close();
        return result;
    }

    private static PdfSigner GetPdfSigner(Signer signerInfo, PdfReader reader, ByteArrayOutputStream output) throws IOException {
        PdfSigner signer = new PdfSigner(reader, output, new StampingProperties());
        String contact = signerInfo.dbj_cedula + " - " + signerInfo.dbj_nombres + " " + signerInfo.dbj_apellidos;
        String fullName = signerInfo.dbj_nombres + " " + signerInfo.dbj_apellidos;
        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance.setReason(signerInfo.reason);
        appearance.setLocation(signerInfo.location);
        appearance.setSignatureCreator("NexumSign");
        appearance.setContact(contact);
        appearance.setPageNumber(signerInfo.position.num_page);
        appearance.setPageRect(new Rectangle(signerInfo.position.x, signerInfo.position.y, 300, 80));
        Date date = new Date();

        ImageData back = ImageDataFactory.create("./fondo.png");
        appearance.setImage(back);
        appearance.setImageScale(0.3F);

        /*appearance.setLayer2Font(PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN));*/
        String text = "Firmante: \n" + fullName + "\nNo. Documento: " + signerInfo.dbj_cedula
                + "\nTIMESTAMP: " + date.getTime() + "\nRol: " + signerInfo.attribute_header
                + "\nFace ID: " + signerInfo.face_id + "\nZone: " + signerInfo.location;
        appearance.setLayer2Text(text);
        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC_AND_DESCRIPTION);

        byte[] img = Base64.getDecoder().decode(signerInfo.image_sign.encode);
        ImageData image = ImageDataFactory.create(img);
        appearance.setSignatureGraphic(image);

        signer.setCertificationLevel(1);
        signer.setFieldName(signerInfo.dbj_cedula);
        return signer;
    }
}
