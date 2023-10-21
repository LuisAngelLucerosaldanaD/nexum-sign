package com.nexum.sign.models;

public class RequestSign {
    public String pdf;
    public String location;
    public String document_id;

    public boolean validate() {
        return pdf != null && !pdf.isEmpty() && location != null && !location.isEmpty() && document_id != null
                && !document_id.isEmpty();
    }

}
