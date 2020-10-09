package com.cpt22.HTTPServer;

public enum LogLevel {
    DEBUG("DEBUG", 0),
    VERBOSE("VERBOSE", 1),
    INFO("INFO", 2),
    WARN("WARN", 3),
    SEVERE("SEVERE", 4),
    FATAL("FATAL", 5);

    public final String text;
    public final int val;

    private LogLevel(String text, int val) {
        this.text = text;
        this.val = val;
    }

}
