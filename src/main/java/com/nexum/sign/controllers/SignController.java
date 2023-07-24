package com.nexum.sign.controllers;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.signatures.*;
import com.nexum.sign.models.RequestSign;
import com.nexum.sign.models.RequestValidateSign;
import com.nexum.sign.models.Response;
import com.nexum.sign.utils.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;

@RestController
@RequestMapping("/api/v1/nexum")
@AllArgsConstructor
public class SignController {

    @Operation(summary = "Firma electrónica del PDF generado por Nexum",
            description = "Método que permite firmar eletrónicamente el archivo PDF generado por el sistema Nexum")
    @SecurityRequirement(name = "bearer")
    @Tag(name = "Sign")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/sign")
    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Token de autorización",
            required = true, example = "Bearer <token>")
    public ResponseEntity<Response> SignPdf(@RequestBody RequestSign req) {

        Response res = new Response(true);

        if (req == null || !req.validate()) {
            res.msg = "Cuerpo de la petición no valida";
            res.code = 1;
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        try {

            String originalPdfFilePath = "./temp_signed.pdf";
            String signedPdfFilePath = "./temp_signeded.pdf";
            String keystoreFilePath = "./test.p12";
            String keystorePassword = "comfandi_datacredito_dev";
            String certAlias = "datacredito_comfandi_dev";

            FileUtil.base64ToStore(req.pdf, originalPdfFilePath);

            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystoreFilePath)) {
                keystore.load(fis, keystorePassword.toCharArray());
            }

            Certificate[] chain = keystore.getCertificateChain(certAlias);
            PrivateKey privateKey = (PrivateKey) keystore.getKey(certAlias, keystorePassword.toCharArray());

            sign(originalPdfFilePath, signedPdfFilePath, chain, privateKey, DigestAlgorithms.SHA256,
                    PdfSigner.CryptoStandard.CMS, req.location, req.ip_list, req.document_id);

            res.error = false;
            res.data = "acá ira el base 64";
            res.code = 29;
            res.type = "success";
            res.msg = "procesado correctamente";
            res.data = FileUtil.getBase64ToPath(signedPdfFilePath);

            FileUtil.deleteFile(originalPdfFilePath);
            FileUtil.deleteFile(signedPdfFilePath);
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
        } catch (Exception e) {
            res.msg = e.getMessage();
            res.code = e.hashCode();
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
        }
    }

    @Operation(summary = "Valida la firma electrónica del PDF generado por Nexum",
            description = "Método que permite validar la firmar eletrónicamente del archivo PDF generado por el sistema Nexum")
    @Tag(name = "Sign")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/validate-sign")
    public ResponseEntity<Response> ValidateSignPdf(
            @RequestBody
            RequestValidateSign req
    ) {
        Response res = new Response(true);
        if (req == null || req.pdf == null) {
            res.msg = "Cuerpo de la petición no valida";
            res.code = 1;
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        String originalPdfFilePath = "./temp-validate.pdf";

        try {
            FileUtil.base64ToStore(req.pdf, originalPdfFilePath);

            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
        } catch (Exception e) {

            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
        }
    }

    public void sign(String src, String dest, Certificate[] chain,
                     PrivateKey pk, String digestAlgorithm,
                     PdfSigner.CryptoStandard subFilter, String location, String ipList, String documentID)
            throws GeneralSecurityException, IOException {

        PdfReader reader = new PdfReader(src);
        FileOutputStream file = new FileOutputStream(dest);
        PdfSigner signer = new PdfSigner(reader, file, new StampingProperties());

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance.setReason("Firma electronica expedida por www.nexum.com");
        appearance.setLocation(location);
        appearance.setSignatureCreator("NexumSign");
        appearance.setContact("souport@nexum.sign - 51923062749");
        appearance.setReuseAppearance(false);

        signer.setCertificationLevel(1);
        signer.setFieldName("sig");

        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        IExternalSignature pks = new PrivateKeySignature(pk, digestAlgorithm, BouncyCastleProvider.PROVIDER_NAME);
        IExternalDigest digest = new BouncyCastleDigest();

        signer.signDetached(digest, pks, chain, null, null, null, 0, subFilter);
        file.close();
        reader.close();
    }

}
