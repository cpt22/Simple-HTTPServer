package com.cpt22.HTTPServer;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientConnection implements Runnable {
    private static File WEB_ROOT;
    private HTTPServer server;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedOutputStream bytesOut;
    private Configuration config;

    public ClientConnection(Socket socket, HTTPServer server) {
        this.socket = socket;
        this.server = server;
        this.config = server.getConfig();
        try {
            WEB_ROOT = new File(config.getSection("files").getString("web-root", "."));
        } catch (YAMLConfigurationException e) {
            e.printStackTrace();
        }

        // If the config value for persistent connections is set, then set the socket timeout to a non-zero value as specified in the config.
        // Setting a timeout will cause the socket to throw a timeout exception after the specified amount of milliseconds.
        if (server.areConnectionsPersistent()) {
            try {
                socket.setSoTimeout(server.getPersistentConnectionTimeout());
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public HTTPServer getServer() {
        return server;
    }

    @Override
    public void run() {

        // While the socket is not timed out or closed, listen for and process HTTP requests
        while(true) {
            List<String> headers = new ArrayList<>();
            Map<String, String> headerMap = new HashMap<>();
            Map<String, String> cookies;
            try {
                // Open input and output streams/writers to facilitate communication with the client
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());
                bytesOut = new BufferedOutputStream(socket.getOutputStream());

                // Read all of the headers line by line into the list of headers
                String inputLine;
                while (!(inputLine = in.readLine()).equals("")) {
                    headers.add(inputLine);
                }

                // Add all headers to map for easier retrieval
                for (int i = 1; i < headers.size(); i++) {
                    String[] arr = headers.get(i).split(": ");
                    headerMap.put(arr[0].toLowerCase(), arr[1]);
                }

                // Parse all of the cookies and store them in the cookie Map
                cookies = Utility.parseCookies(headers);

                getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.VERBOSE, "Request received from client");

                // Debug headers and cookies
                if (server.isDebugging()) {
                    getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.DEBUG, "RECEIVED HEADERS");
                    for (String header : headers) {
                        getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.DEBUG, header);
                    }

                    for (Map.Entry<String, String> entry : cookies.entrySet()) {
                        getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.DEBUG, "cookie: " + entry.getKey() + "=" + entry.getValue());
                    }
                }

                // If the config value for persistent connections is set, then set the socket timeout to the value specified in the headers otherwise use server config val.
                if (server.areConnectionsPersistent() && headerMap.containsKey("keep-alive")) {
                    try {
                        String hdr = headerMap.get("keep-alive");
                        String[] attrSplit = hdr.split(",");
                        Map<String, String> tempMap = new HashMap<>();
                        for (String s : attrSplit) {
                            String[] tmp = s.split("=");
                            tempMap.put(tmp[0], tmp[1]);
                        }
                        socket.setSoTimeout(Integer.parseInt(tempMap.get("timeout")) * 1000);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.VERBOSE, "Malformed headers received");
                    }
                }

                String method = headers.get(0).split(" ")[0];
                String filePath = headers.get(0).split(" ")[1];

                if (filePath.substring(filePath.length() - 1).equals("/")) {
                    filePath += config.getString("index-page", "index.html");
                }

                // This server only handles GET requests for now
                if (method.equalsIgnoreCase("GET")) {

                    // Loads the file at the specified filePath
                    File file = new File(WEB_ROOT + filePath);
                    if (!Files.exists(Paths.get(WEB_ROOT + filePath))) {
                        getLogger().logRequest(socket.getInetAddress().getHostAddress(), socket.getInetAddress().getHostName(), headers.get(0), String.valueOf(ResponseCode.FILE_NOT_FOUND.code));
                        throw new FileNotFoundException();
                    }

                    // Track visits based on the cookie stored
                    int visitCount = 1;
                    if (cookies.containsKey("visits") && Utility.isNumeric(cookies.get("visits"))) {
                        visitCount = Integer.parseInt(cookies.get("visits")) + 1;
                    }

                    // Sets up symbols for replacement within HTML files. This is used to insert the number of visits dynamically into the visits.html
                    Map<String, Object> symbolMap = new HashMap<>();
                    switch (filePath) {
                        case "/cpt15/visits.html":
                            symbolMap.put("visits", visitCount);
                            break;
                        default:
                            break;
                    }

                    // Uses the FileParser to parse the html with the symbol map and perform necessary replacements
                    String fileString = FileParser.parseFile(file, symbolMap);

                    // Specifies additional headers to send (Such as set-cookie headers) along with the standard headers
                    List<String> additionalHeaders = new ArrayList<>();
                    additionalHeaders.add(Utility.buildCookie("visits", String.valueOf(visitCount), "Path: cpt15"));

                    // Sends the HTTP response
                    getLogger().logRequest(socket.getInetAddress().getHostAddress(), socket.getInetAddress().getHostName(), headers.get(0), String.valueOf(ResponseCode.OK.code));
                    if (!sendResponse(ResponseCode.OK, additionalHeaders, fileString)) {
                        getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.INFO, "Error sending response to client");
                    }
                } else {
                    // Send this response if the client sends a request with an unsupported method
                    if (!sendResponse(ResponseCode.OK, new File(WEB_ROOT, "method-not-supported.html"))) {
                        getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.INFO, "Error sending response to client");
                    }
                }

                // If connections are not persistent, close all socket connections.
                if (!server.areConnectionsPersistent()) {
                    getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.VERBOSE, "Connection with a client closed (non-persistent)");
                    in.close();
                    bytesOut.close();
                    out.close();
                    socket.close();
                    break;
                }
            } catch (FileNotFoundException ex) {
                try {
                    if(!sendResponse(ResponseCode.FILE_NOT_FOUND, "<html><head><title>Page not found</title></head><body>404 Page not found</body></html>")){//new File(WEB_ROOT, ResponseCode.FILE_NOT_FOUND.path))) {
                        getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.INFO, "Error sending response to client");
                    }
                } catch (IOException ex1) {
                    getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.SEVERE, "An exception was encountered in generating the 404 error page " + ex1.getMessage());
                    System.err.println("An exception was encountered in generating the error page: " + ex1.getMessage());
                }
            } catch (SocketTimeoutException ex) {
                // Catch the SocketTimeoutException and close all of the streams and socket.
                getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.VERBOSE, "Connection with a client timed out and was closed (persistent)");
                try {
                    in.close();
                    bytesOut.close();
                    out.close();
                    socket.close();
                } catch (IOException ex1) {
                    getLogger().errorLog("", LogLevel.WARN, ex1.getMessage());
                }
                break;
            } catch (IOException ex) {
                getLogger().errorLog("", LogLevel.WARN, ex.getMessage());
                break;
            }
        }
    }

    public Logger getLogger() {
        return server.getLogger();
    }

    /**
     * Sends a response to the client provided a response code and a file to send
     * @param code - Response code to send
     * @param file - The file to send
     * @throws IOException - Thrown if there is an issue writing to the streams
     */
    private boolean sendResponse(ResponseCode code, Object file) throws IOException {
        return sendResponse(code, null, file);
    }

    /**
     * Sends a response to the client provided a response code, additional headers, and a file to send
     * @param code - Response code to send
     * @param extraHeaders - Extra headers such as cookies
     * @param file - The file to send
     * @throws IOException - Thrown if there is an issue writing to the streams
     */
    private boolean sendResponse(ResponseCode code, List<String> extraHeaders, Object file) throws IOException {
        int fileLength;
        byte[] fileData;

        // Sometimes a file is provided (in the case of 404) but other times the file is provided in the form of a String (the result of the FileParser)
        if (file instanceof File) {
            fileLength = (int) ((File) file).length();
            fileData = Utility.fileToByteArray((File) file);
        } else if (file instanceof String) {
            fileData = ((String) file).getBytes(StandardCharsets.UTF_8);
            fileLength = fileData.length;
        } else {
            return false;
        }

        // Builds and sends the header using the helper method in the Utility class
        String headers = Utility.buildHeaders(this, code, extraHeaders, "text/html", fileLength);

        getLogger().errorLog(socket.getInetAddress().getHostAddress(), LogLevel.DEBUG, "SENDING HEADERS\n" + headers);

        out.print(headers);
        out.flush();

        // Writes the file/string data in byte array form
        bytesOut.write(fileData, 0, fileLength);
        bytesOut.flush();
        return true;
    }
}
