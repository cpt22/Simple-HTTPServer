package com.cpt22.HTTPServer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class is used to log http requests into the specified log file
 */
public class Logger {
    private HTTPServer server;
    private SimpleDateFormat dateFormatter;
    private Configuration configSection;

    private boolean enabled;

    private PrintWriter httpLogFileWriter;


    public Logger(HTTPServer server) {
        this.server = server;
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        try {
            this.configSection = server.getConfig().getSection("logging");
            this.httpLogFileWriter = new PrintWriter(new FileWriter(configSection.getString("log-file-path", "./http-log"), true));
            this.enabled = configSection.getBoolean("enabled", true);
        } catch (YAMLConfigurationException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logRequest(String ip, String hostname, String request, String status) {
        if (enabled) {
            httpLogFileWriter.printf("%-27s %-40s %-23s %-60s %7s %n", ip, hostname, dateFormatter.format(new Date()), request, status);
            httpLogFileWriter.flush();
        }
    }
}
