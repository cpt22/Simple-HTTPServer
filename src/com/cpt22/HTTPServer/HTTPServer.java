package com.cpt22.HTTPServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HTTPServer implements Runnable {
    private boolean debug = false;
    private boolean persistentConnections = false;
    private int persistentConnectionTimeout = 0;
    private String version = "";

    private int port;
    private String bindAddr;
    private ServerSocket servSock;
    private ThreadPoolExecutor executor;
    private Configuration config;

    private Logger logger;


    public HTTPServer() {
        config = new Configuration();

        loadConfig();

        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        this.logger = new Logger(this);

        // Open ServerSocket to listen for connections.
        try {
            servSock = new ServerSocket();
            servSock.bind(new InetSocketAddress(InetAddress.getByName(bindAddr), port));
            System.out.println("HTTP Server started " + (config.getBoolean("debug", false) ? "IN DEBUG MODE " : "") + "using " + (config.getSection("persistence").getBoolean("use-persistent-connections", true) ? "persistent connections" : "non-persistent connections"));
            System.out.println("Listening for requests on " + bindAddr + ":" + port);
            this.run();
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        } catch (YAMLConfigurationException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public boolean isDebugging() {
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

    public Logger getLogger() { return logger; }

    /**
     * Load the values from the configuration file into the server
     */
    private void loadConfig() {
        try {
            this.debug = config.getBoolean("debug", false);
            this.persistentConnections = config.getSection("persistence").getBoolean("use-persistent-connections", false);
            this.persistentConnectionTimeout = config.getSection("persistence").getInt("persistent-connection-timeout", 6000);
            this.bindAddr = config.getString("bind-address", "0.0.0.0");
            this.port = config.getInt("port", 80);
            this.version = config.getString("server-version");
        } catch (YAMLConfigurationException ex) {
            System.err.println(ex.getMessage());
        }
    }

    /**
     * Begin accepting socket requests from the clients. Upon receiving a connection from a client, create a new ClientConnection object and dispatch it to the CachedThreadPool for execution
     */
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
