package com.nexum.sign.models.signer;

import lombok.AllArgsConstructor;

import java.security.PrivateKey;
import java.security.cert.Certificate;

@AllArgsConstructor
public class SignerCertificate {
    public Certificate[] certificates;
    public PrivateKey privateKey;
}
