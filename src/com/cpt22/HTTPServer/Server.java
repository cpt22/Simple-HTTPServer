package com.cpt22.HTTPServer;

public class Server {
    /**
     * Static methods to instantiate server
     */
    private static HTTPServer server;
    public static void main(String[] args) {
        server = new HTTPServer();
    }

    public static HTTPServer getServer() {
        return server;
    }
}
