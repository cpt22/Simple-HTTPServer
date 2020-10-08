package com.cpt22.HTTPServer;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientConnection implements Runnable {
    static File WEB_ROOT;
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
            WEB_ROOT = new File(config.getSection("files").getString("root-dir", "."));
        } catch (YAMLConfigurationException e) {
            e.printStackTrace();
        }
    }

    public HTTPServer getServer() {
        return server;
    }

    @Override
    public void run() {
        // If the config value for persistent connections is set, then set the socket timeout to a non-zero value as specified in the config.
        // Setting a timeout will cause the socket to throw a timeout exception after the specified amount of milliseconds.
        if (server.areConnectionsPersistent()) {
            try {
                socket.setSoTimeout(server.getPersistentConnectionTimeout());
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        // While the socket is not timed out or closed, listen for and process HTTP requests
        while(true) {
            List<String> headers = new ArrayList<String>();
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

                // Parse all of the cookies and store them in the cookie Map
                cookies = Utility.parseCookies(headers);

                if (server.isDebugging())
                    System.out.println("Request received from client");
                // Debug headers and cookies
                if (server.isDebugging()) {
                    System.out.println("\nRECEIVED HEADERS:");
                    for (int i = 0; i < headers.size(); i++) {
                        System.out.println(headers.get(i));
                    }

                    for (Map.Entry<String, String> entry : cookies.entrySet()) {
                        System.out.println("cookie: " + entry.getKey() + "=" + entry.getValue());
                    }
                }


                String method = headers.get(0).split(" ")[0];
                String filePath = headers.get(0).split(" ")[1];
                // Strip preceding / from request URL
                if (filePath.indexOf("/") == 0) {
                    filePath = filePath.substring(1);
                }

                // This server only handles GET requests for now
                if (method.equalsIgnoreCase("GET")) {
                    // Loads the file at the specified filePath
                    File file = new File(WEB_ROOT, filePath);
                    if (file.length() == 0) {
                        server.getLogger().logRequest(socket.getInetAddress().getHostAddress(), socket.getInetAddress().getHostName(), headers.get(0), String.valueOf(ResponseCode.FILE_NOT_FOUND.code));
                        throw new FileNotFoundException();
                    }

                    // Track visits based on the cookie stored
                    int visitCount = 1;
                    if (cookies.containsKey("visits") && Utility.isNumeric(cookies.get("visits"))) {
                        visitCount = Integer.parseInt(cookies.get("visits")) + 1;
                    }

                    // Sets up symbols for replacement within HTML files. This is used to insert the number of visits dynamically into the visits.html
                    Map<String, Object> symbolMap = new HashMap<String, Object>();
                    switch (filePath) {
                        case "cpt15/visits.html":
                            symbolMap.put("visits", visitCount);
                            break;
                        default:
                            break;
                    }

                    // Uses the FileParser to parse the html with the symbol map and perform necessary replacements
                    String fileString = FileParser.parseFile(file, symbolMap);

                    // Specifies additional headers to send (Such as set-cookie headers) along with the standard headers
                    List<String> additionalHeaders = new ArrayList<String>();
                    additionalHeaders.add(Utility.buildCookie("visits", String.valueOf(visitCount), "Path: cpt15"));

                    // Sends the HTTP response
                    server.getLogger().logRequest(socket.getInetAddress().getHostAddress(), socket.getInetAddress().getHostName(), headers.get(0), String.valueOf(ResponseCode.OK.code));
                    sendResponse(ResponseCode.OK, additionalHeaders, fileString);
                } else {
                    // Send this response if the client sents a request with an unsupported method
                    sendResponse(ResponseCode.OK, new File(WEB_ROOT, "method-not-supported.html"));
                }

                // If connections are not persistent, close all socket connections.
                if (!server.areConnectionsPersistent()) {
                    if (server.isDebugging())
                        System.out.println("Connection with a client closed");
                    in.close();
                    bytesOut.close();
                    out.close();
                    socket.close();
                    break;
                }
            } catch (FileNotFoundException ex) {
                try {
                    // Send the 404 response and error page if the specified URL is not found
                    sendResponse(ResponseCode.FILE_NOT_FOUND, new File(WEB_ROOT, ResponseCode.FILE_NOT_FOUND.path));
                } catch (IOException ioex) {
                    System.err.println("An exception was encountered in generating the error page: " + ioex.getMessage());
                }
            } catch (SocketTimeoutException ex) {
                // Catch the SocketTimeoutException and close all of the streams and socket.
                if (server.isDebugging())
                    System.out.println("Connection with a client timed out");
                try {
                    in.close();
                    bytesOut.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            }
        }
    }


    /**
     * Sends a response to the client provided a response code and a file to send
     * @param code - Response code to send
     * @param file - The file to send
     * @throws IOException
     */
    private void sendResponse(ResponseCode code, Object file) throws IOException {
        sendResponse(code, null, file);
    }

    /**
     * Sends a response to the client provided a response code, additional headers, and a file to send
     * @param code - Response code to send
     * @param extraHeaders - Extra headers such as cookies
     * @param file - The file to send
     * @throws IOException
     */
    private boolean sendResponse(ResponseCode code, List<String> extraHeaders, Object file) throws IOException {
        int fileLength;
        byte[] fileData;

        // Sometimes a file is provided (in the case of 404) but other times the file is provided in the form of a String (the result of the FileParser)
        if (file instanceof File) {
            fileLength = (int) ((File) file).length();
            fileData = Utility.fileToByteArray((File) file);
        } else if (file instanceof String) {
            fileData = ((String) file).getBytes("UTF-8");
            fileLength = fileData.length;
        } else {
            return false;
        }

        // Builds and sends the header using the helper method in the Utility class
        String headers = Utility.buildHeaders(this, code, extraHeaders, "text/html", fileLength);

        if (server.isDebugging()) {
            System.out.println("\nSENDING HEADERS:");
            System.out.println(headers);
        }

        out.print(headers);
        out.flush();

        // Writes the file/string data in byte array form
        bytesOut.write(fileData, 0, fileLength);
        bytesOut.flush();
        return true;
    }
}
