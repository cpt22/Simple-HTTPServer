package com.cpt22.HTTPServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HTTPServer implements Runnable {
    private boolean debug = false;
    private boolean persistentConnections = false;
    private int persistentConnectionTimeout = 0;
    private String version = "";

    private int port;
    private ServerSocket servSock;
    private ThreadPoolExecutor executor;
    private Configuration config;


    public HTTPServer() {
        config = new Configuration();

        loadConfig();
        System.out.println(config);
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        try {
            servSock = new ServerSocket(port);
            System.out.println("HTTP Server Started.");
            System.out.println("Listening for requests on port " + port);
            this.run();
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isDebugMode() {
        return debug;
    }

    public boolean areConnectionsPersistent() {
        return persistentConnections;
    }

    public int getPersistentConnectionTimeout() {
        return persistentConnectionTimeout;
    }

    public String getVersion() {
        return version;
    }

    public Configuration getConfig() {
        return config;
    }


    private void loadConfig() {
        try {
            this.debug = config.getBoolean("debug", false);
            this.persistentConnections = config.getSection("persistence").getBoolean("use-persistent-connections", false);
            this.persistentConnectionTimeout = config.getSection("persistence").getInt("persistent-connection-timeout", 6000);
            this.port = config.getInt("port");
            this.version = config.getString("server-version");
        } catch (YAMLConfigurationException ex) {
            System.err.println(ex.getMessage());
        }
    }


    @Override
    public void run() {
        while(true) {
            try {
                executor.submit(new ClientConnection(servSock.accept(), this));
                if (debug)
                    System.out.println("Connection established with Client");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Static methods to instantiate server
     */
    private static HTTPServer server;
    public static void main(String[] args) {
        server = new HTTPServer();
    }


}
