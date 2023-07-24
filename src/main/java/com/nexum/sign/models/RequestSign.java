package com.nexum.sign.models;

public class RequestSign {
    public String pdf;
    public String location;
    public String document_id;
    public String ip_list;


    public boolean validate() {
        return pdf != null && !pdf.equals("") && location != null && !location.equals("") && document_id != null
                && !document_id.equals("") && ip_list != null && !ip_list.equals("");
    }

}
