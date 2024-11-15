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
        String hash = FileUtil.getFileHash(pdf);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));

        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        IExternalDigest digest = new BouncyCastleDigest();
        IExternalSignature pks = new PrivateKeySignature(pk, digestAlgorithm, BouncyCastleProvider.PROVIDER_NAME);

        for (Signer signer : signers) {
            PdfSigner pdfSigner = GetPdfSigner(signer, reader, output, hash);
            pdfSigner.setCertificationLevel(PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED);
            pdfSigner.signDetached(digest, pks, chain, null, null, null, 0, subFilter);
        }

        String result = Base64.getEncoder().encodeToString(output.toByteArray());
        output.close();
        reader.close();
        return result;
    }

    private static PdfSigner GetPdfSigner(Signer signerInfo, PdfReader reader, ByteArrayOutputStream output, String hash) throws IOException {
        PdfSigner signer = new PdfSigner(reader, output, new StampingProperties());
        String contact = signerInfo.dbj_cedula + " - " + signerInfo.dbj_nombres + " " + signerInfo.dbj_apellidos;
        String fullName = signerInfo.dbj_nombres + " " + signerInfo.dbj_apellidos;
        Date date = new Date();

        signer.setReason(signerInfo.reason);
        signer.setLocation(signerInfo.location);
        signer.setSignatureCreator("NexumSign");
        signer.setContact(contact);
        signer.setPageRect(new Rectangle(signerInfo.position.x, signerInfo.position.y, 300, 70));

        String text = "Hash: \n" + hash + "\nFirmante: \n" + fullName + "\nNo. Documento: " + signerInfo.dbj_cedula
                + "\nTIMESTAMP: " + date.getTime() + "\nRol: " + signerInfo.attribute_header
                + "\nFace ID: " + signerInfo.face_id + "\nZone: " + signerInfo.location;

        SignatureFieldAppearance appearance = new SignatureFieldAppearance(signerInfo.dbj_cedula);
        appearance.setPageNumber(signerInfo.position.num_page);
        byte[] img = Base64.getDecoder().decode(signerInfo.image_sign.encode);
        ImageData image = ImageDataFactory.create(img);
        appearance.setContent(text, image);

        /*ImageData back = ImageDataFactory.create("./fondo.png");
        BackgroundSize size = new BackgroundSize();
        size.setBackgroundSizeToValues(UnitValue.createPercentValue(50), UnitValue.createPercentValue(30));

        appearance.setBackgroundImage(
                new BackgroundImage.Builder()
                        .setImage(new PdfImageXObject(back))
                        .setBackgroundSize(size)
                        .build());*/

        signer.setSignatureAppearance(appearance);
        signer.setCertificationLevel(1);
        signer.setFieldName(signerInfo.dbj_cedula);
        return signer;
    }
}
