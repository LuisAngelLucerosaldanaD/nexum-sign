package com.nexum.sign.controllers;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import com.nexum.sign.models.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import com.itextpdf.signatures.BouncyCastleDigest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.List;

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
    public ResponseEntity<Response> SignPdf(
            @RequestPart("pdf") MultipartFile pdfFile,
            @RequestParam("location") String location,
            @RequestParam("document_id") String documentId
    ) {

        Response res = new Response(true);

        if (location == null || location.isEmpty() || documentId == null || documentId.isEmpty()) {
            res.msg = "Cuerpo de la petición no valida";
            res.code = 1;
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        try {

            String keystoreFilePath = "./nexum.p12";
            String keystorePassword = "nexum123";
            String certAlias = "nexum";

            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystoreFilePath)) {
                keystore.load(fis, keystorePassword.toCharArray());
            }

            Certificate[] chain = keystore.getCertificateChain(certAlias);
            PrivateKey privateKey = (PrivateKey) keystore.getKey(certAlias, keystorePassword.toCharArray());

            String pdfSigned = sign(pdfFile, chain, privateKey, DigestAlgorithms.SHA256,
                    PdfSigner.CryptoStandard.CMS, location, documentId);

            res.error = false;
            res.code = 29;
            res.type = "success";
            res.msg = "procesado correctamente";
            res.data = pdfSigned;

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
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/validate-sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response> ValidateSignPdf(
            @RequestParam("pdf") MultipartFile pdf
    ) {
        Response res = new Response(true);

        try {

            BouncyCastleProvider provider = new BouncyCastleProvider();
            Security.addProvider(provider);

            String keystoreFilePath = "./nexum.p12";
            String keystorePassword = "nexum123";
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystoreFilePath)) {
                keystore.load(fis, keystorePassword.toCharArray());
            }

            PdfReader pdfReader = new PdfReader(pdf.getInputStream());
            PdfDocument pdfDoc = new PdfDocument(pdfReader);

            SignatureUtil signUtil = new SignatureUtil(pdfDoc);
            List<String> names = signUtil.getSignatureNames();

            if (names.isEmpty()) {
                res.msg = "No se encontraron firmas en el documento pdf";
                res.code = 1;
                res.type = "error";
                return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
            }

            for (String name : names) {
                PdfPKCS7 signature = signUtil.readSignatureData(name);
                if (!signature.verifySignatureIntegrityAndAuthenticity()) {
                    res.msg = "Las firmas del documento estan corruptas o no son validas";
                    res.code = 1;
                    res.type = "error";
                    return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
                }
            }

            pdfReader.close();
            pdfDoc.close();
            res.error = false;
            res.msg = "Todas las firmas del documento son validas";
            res.code = 29;
            res.type = "success";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
        } catch (Exception e) {
            res.msg = e.getMessage();
            res.code = e.hashCode();
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
        }
    }

    public String sign(MultipartFile pdf, Certificate[] chain,
                       PrivateKey pk, String digestAlgorithm,
                       PdfSigner.CryptoStandard subFilter, String location, String documentID)
            throws GeneralSecurityException, IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(pdf.getInputStream());
        PdfSigner signer = new PdfSigner(reader, output, new StampingProperties());

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
        String result = Base64.getEncoder().encodeToString(output.toByteArray());
        output.close();
        reader.close();
        return result;
    }

}
