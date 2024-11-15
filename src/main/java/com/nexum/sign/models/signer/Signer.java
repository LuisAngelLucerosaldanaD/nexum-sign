package com.nexum.sign.models.signer;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class Signer {
    public String attribute_header;
    public String dbj_nombres;
    public String dbj_apellidos;
    public String dbj_cedula;
    public String face_id;
    public ImageSign image_sign;
    public Position position;
    public String reason;
    public String location;
    public List<AcrossField> across_fields;
}