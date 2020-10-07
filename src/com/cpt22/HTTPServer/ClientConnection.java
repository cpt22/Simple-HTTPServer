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
    static final File WEB_ROOT = new File(".");
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
    }

    public HTTPServer getServer() {
        return server;
    }

    @Override
    public void run() {
        if (server.areConnectionsPersistent()) {
            try {
                socket.setSoTimeout(server.getPersistentConnectionTimeout());
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        while(true) {
            List<String> headers = new ArrayList<String>();
            Map<String, String> cookies;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());
                bytesOut = new BufferedOutputStream(socket.getOutputStream());

                String inputLine;
                while (!(inputLine = in.readLine()).equals("")) {
                    headers.add(inputLine);
                }

                // Parse all of the cookies
                cookies = Utility.parseCookies(headers);

                // Debug headers and cookies
                if (server.isDebugMode()) {
                    for (int i = 0; i < headers.size(); i++) {
                        System.out.println(headers.get(i));
                    }

                    for (Map.Entry<String, String> entry : cookies.entrySet()) {
                        System.out.println("cookie: " + entry.getKey() + "=" + entry.getValue());
                    }
                }

                // Begin handling url
                String method = headers.get(0).split(" ")[0];
                String filePath = headers.get(0).split(" ")[1];
                if (filePath.indexOf("/") == 0) {
                    filePath = filePath.substring(1);
                }

                if (method.equalsIgnoreCase("GET")) {
                    File file = new File(WEB_ROOT, filePath);
                    if (file.length() == 0) {
                        throw new FileNotFoundException();
                    }

                    int visitCount = 1;
                    if (cookies.containsKey("visits") && Utility.isNumeric(cookies.get("visits"))) {
                        visitCount = Integer.parseInt(cookies.get("visits")) + 1;
                    }

                    Map<String, String> symbolMap = new HashMap<String, String>();

                    switch (filePath) {
                        case "cpt15/visits.html":
                            symbolMap.put("visits", visitCount + "");
                            break;
                        default:
                            break;
                    }

                    String fileString = FileParser.parseFile(file, symbolMap);

                    List<String> additionalHeaders = new ArrayList<String>();
                    additionalHeaders.add(Utility.buildCookie("visits", String.valueOf(visitCount), "Path: cpt15"));

                    sendResponse(ResponseCode.OK, additionalHeaders, fileString);
                } else {
                    sendResponse(ResponseCode.OK, new File(WEB_ROOT, "method-not-supported.html"));
                }

                if (!server.areConnectionsPersistent()) {
                    System.out.println("Connection with a client closed (non-persistent)");
                    in.close();
                    bytesOut.close();
                    out.close();
                    socket.close();
                }
            } catch (FileNotFoundException ex) {
                System.out.println(new File(WEB_ROOT, ResponseCode.FILE_NOT_FOUND.path));
                try {
                    sendResponse(ResponseCode.FILE_NOT_FOUND, new File(WEB_ROOT, ResponseCode.FILE_NOT_FOUND.path));
                    System.out.println("GG");
                } catch (IOException ioex) {
                    System.err.println("An exception was encountered in generating the error page: " + ioex.getMessage());
                }
            } catch (SocketTimeoutException ex) {
                if (server.isDebugMode())
                    System.out.println("Connection with a client timed out (persistent)");
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
        if (file instanceof File) {
            fileLength = (int) ((File) file).length();
            fileData = Utility.fileToByteArray((File) file);
        } else if (file instanceof String) {
            fileData = ((String) file).getBytes("UTF-8");
            fileLength = fileData.length;
        } else {
            return false;
        }

        String headers = Utility.buildHeaders(this, code, extraHeaders, "text/html", fileLength);
        System.out.println(headers);
        out.print(headers);
        out.flush();

        bytesOut.write(fileData, 0, fileLength);
        bytesOut.flush();
        return true;
    }
}
