package com.nexum.sign.controllers;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.*;
import com.nexum.sign.infraestructure.configuration.KeystoreConfig;
import com.nexum.sign.models.PdfImage;
import com.nexum.sign.models.RequestSign;
import com.nexum.sign.models.Response;
import com.nexum.sign.utils.FileUtil;
import com.nexum.sign.utils.PdfUtil;
import com.nexum.sign.utils.SignUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/nexum")
@AllArgsConstructor
public class SignController {
    private final KeystoreConfig keystoreConfig;

    @Operation(summary = "Firma electrónica del PDF generado por Nexum",
            description = "Método que permite firmar eletrónicamente el archivo PDF generado por el sistema Nexum")
    @SecurityRequirement(name = "bearer")
    @Tag(name = "Sign")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/sign")
    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Token de autorización",
            required = true, example = "Bearer <token>")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Request body que contiene la información necesaria para firmar el PDF",
            required = true,
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Response<String>> SignPdf(
            @RequestBody RequestSign req
    ) {
        Response<String> res = new Response<>(true);
        if (req.isValid()) {
            res.msg = "Cuerpo de la petición no valida";
            res.code = 1;
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        try {

            String keystoreFilePath = keystoreConfig.getFilePath();
            String keystorePassword = keystoreConfig.getPassword();
            String certAlias = keystoreConfig.getAlias();

            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystoreFilePath)) {
                keystore.load(fis, keystorePassword.toCharArray());
            }

            Certificate[] chain = keystore.getCertificateChain(certAlias);
            PrivateKey privateKey = (PrivateKey) keystore.getKey(certAlias, keystorePassword.toCharArray());

            req.encode = FileUtil.attachAnnexe(req.encode, req.annexes);

            req.encode = FileUtil.configureDocument(req.encode);

            req.encode = FileUtil.SetAcrossFields(req.encode, req.signers);

            String pdfSigned = SignUtil.Sign(req.encode, chain, privateKey, DigestAlgorithms.SHA256,
                    PdfSigner.CryptoStandard.CMS, req.signers);

            res.error = false;
            res.code = 29;
            res.type = "success";
            res.msg = "procesado correctamente";
            res.data = pdfSigned;

            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.OK);
        } catch (Exception e) {
            res.msg = e.getMessage();
            res.code = e.hashCode();
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.ACCEPTED);
        }
    }

    @Operation(summary = "Convierte las páginas del PDF en imágenes",
            description = "Método que permite convertir las páginas del archivo PDF en imágenes")
    @SecurityRequirement(name = "bearer")
    @Tag(name = "Sign")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/pdf-to-img")
    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Token de autorización",
            required = true, example = "Bearer <token>")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Request body que contiene el archivo PDF a convertir en imágenes",
            required = true,
            content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Response<List<PdfImage>>> GetPagesToImg(
            @RequestParam("pdf") MultipartFile pdf
    ) {
        Response<List<PdfImage>> res = new Response<>(true);
        if (pdf.isEmpty()) {
            res.msg = "No se ha enviado el archivo pdf";
            res.code = 1;
            res.type = "error";
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

        try {
            List<PdfImage> images = PdfUtil.getPagesToImg(pdf.getBytes());
            res.error = false;
            res.code = 29;
            res.type = "success";
            res.msg = "procesado correctamente";
            res.data = images;
            return new ResponseEntity<>(res, new HttpHeaders(), HttpStatus.OK);
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
    public ResponseEntity<Response<Boolean>> ValidateSignPdf(
            @RequestParam("pdf") MultipartFile pdf
    ) {
        Response<Boolean> res = new Response<>(true);

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

}
