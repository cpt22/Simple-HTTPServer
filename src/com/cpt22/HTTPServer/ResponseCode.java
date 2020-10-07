package com.cpt22.HTTPServer;

public enum ResponseCode {
    OK(200, "OK", ""),
    FILE_NOT_FOUND(404, "File Not Found", "cpt15/404.html");

    public final String text;
    public final int code;
    public final String path;

    private ResponseCode(int code, String text, String path) {
        this.code = code;
        this.text = text;
        this.path = path;
    }

}
