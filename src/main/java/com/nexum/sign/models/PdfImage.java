package com.nexum.sign.models;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PdfImage {
    public String encoded;
    public int page;
    public String name;
    public float width;
    public float height;
}
