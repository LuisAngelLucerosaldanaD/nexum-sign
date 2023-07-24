package com.nexum.sign.models;

public class Response {
    public boolean error;
    public int code;

    public String data;
    public String msg;
    public String type;

    public Response(boolean error) {
        this.error = error;
        this.code = 0;
        this.msg = "";
        this.data = "";
        this.type = "";
    }

}
