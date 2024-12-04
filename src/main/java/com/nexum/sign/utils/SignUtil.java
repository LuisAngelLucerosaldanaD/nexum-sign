package com.nexum.sign.utils;

import com.itextpdf.forms.form.element.SignatureFieldAppearance;
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
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        byte[] currentPdfBytes = pdfBytes;
        for (Signer signer : signers) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            IExternalDigest digest = new BouncyCastleDigest();
            IExternalSignature pks = new PrivateKeySignature(pk, digestAlgorithm, BouncyCastleProvider.PROVIDER_NAME);

            String hash = FileUtil.getHash(currentPdfBytes);
            PdfReader reader = new PdfReader(new ByteArrayInputStream(currentPdfBytes));
            PdfSigner pdfSigner = GetPdfSigner(signer, reader, output, hash);
            pdfSigner.setCertificationLevel(PdfSigner.NOT_CERTIFIED);
            pdfSigner.signDetached(digest, pks, chain, null, null, null, 0, subFilter);
            currentPdfBytes = output.toByteArray();

            output.close();
            reader.close();
        }

        return Base64.getEncoder().encodeToString(currentPdfBytes);
    }

    private static PdfSigner GetPdfSigner(Signer signerInfo, PdfReader reader, ByteArrayOutputStream output, String hash) throws IOException {
        StampingProperties properties = new StampingProperties();
        properties.useAppendMode();
        PdfSigner signer = new PdfSigner(reader, output, properties);
        String fullName = signerInfo.dbj_nombres + " " + signerInfo.dbj_apellidos;
        String contact = signerInfo.dbj_cedula + " - " + fullName;
        Date date = new Date();

        signer.setReason(signerInfo.reason);
        signer.setLocation(signerInfo.location);
        signer.setSignatureCreator("NexumSign");
        signer.setContact(contact);
        signer.setPageRect(new Rectangle(signerInfo.position.x, signerInfo.position.y, 300, 70));
        long timestamp = date.getTime();

        String text = String.format("Hash: %s\nFirmante: %s\nNo. Documento: %s\nTIMESTAMP: %d\nRol: %s\nFace ID: %s\nZone: %s", hash, fullName,
                signerInfo.dbj_cedula, timestamp, signerInfo.attribute_header, signerInfo.face_id, signerInfo.location);

        SignatureFieldAppearance appearance = new SignatureFieldAppearance(signerInfo.dbj_cedula);
        appearance.setPageNumber(signerInfo.position.num_page);
        byte[] img = Base64.getDecoder().decode(signerInfo.image_sign.encode);
        ImageData image = ImageDataFactory.create(img);
        appearance.setContent(text, image);

        signer.setSignatureAppearance(appearance);
        signer.setFieldName(signerInfo.dbj_cedula + "-" + timestamp);
        return signer;
    }
}
