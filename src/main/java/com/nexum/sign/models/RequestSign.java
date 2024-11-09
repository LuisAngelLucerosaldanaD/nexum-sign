package com.nexum.sign.models;

import com.nexum.sign.models.annexe.Annexe;
import com.nexum.sign.models.annexe.FilePdf;
import com.nexum.sign.models.signer.Signer;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class RequestSign {
    public String original;
    public String description;
    public String encode;
    public List<Annexe> annexes;
    public List<Signer> signers;

    public boolean isValid() {
        return original == null || description == null || encode == null || encode.isEmpty() || annexes == null || signers == null || signers.isEmpty();
    }
}