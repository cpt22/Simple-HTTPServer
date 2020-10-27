package com.cpt22.HTTPServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utility {

    /**
     * Helper method to build the header string
     * @param cc - client connection object, used to get certain server info
     * @param rc - the response code to send to the client (200, 404, etc.)
     * @param extraHeaders - a list of extra headers such as set-cookie etc.
     * @param mimeType - the mime type of the returned data
     * @param contentLength - the length (in bytes) of the content returned
     * @return - A string containing all of the headers delimited by a newline
     */
    public static String buildHeaders(ClientConnection cc, ResponseCode rc, List<String> extraHeaders, String mimeType, int contentLength) {
        String ret = "";
        ret += "HTTP/1.1 " + rc.code + " " + rc.text + "\r\n";
        ret += "Server: Christian Tingles HTTP Server v" + cc.getServer().getVersion() + "\r\n";
        ret += "Date: " + new Date() + "\r\n";
        ret += "Content-type: " + mimeType + "\r\n";
        ret += "Content-length: " + contentLength + "\r\n";
        if (!cc.getServer().areConnectionsPersistent())
            ret += "Connection: close\r\n";
        if (extraHeaders != null) {
            for (int i = 0; i < extraHeaders.size(); i++) {
                ret += extraHeaders.get(i) + "\r\n";
            }
        }
        ret += "\r\n";
        return ret;
    }

    /**
     * Converts the provided file into an array of bytes to be send to the client using a BufferedOutputStream
     * @param file - File to be sent
     * @return - Array of bytes making up the file
     * @throws IOException
     */
    public static byte[] fileToByteArray(File file) throws IOException {
        FileInputStream fis = null;
        byte[] data = new byte[(int) file.length()];

        try {
            fis = new FileInputStream(file);
            fis.read(data);
        } finally {
            if (fis != null)
                fis.close();
        }

        return data;
    }

    /**
     * The function traverses the list of headers to find each line that begins with "Cookie: ". Then it splits up that line into key value pairs which it puts into the cookie map.
     * @param headers
     * @return
     */
    public static Map<String, String> parseCookies(List<String> headers) {
        Map<String, String> cookieMap = new HashMap<String, String>();
        for (String header : headers) {
            if (header.contains("Cookie: ")) {
                String line = header.substring(8);
                String[] cookies = line.split("; ");
                for (int i = 0; i < cookies.length; i++) {
                    String[] cookie = cookies[i].split("=");
                    cookieMap.put(cookie[0], cookie.length > 1 ? cookie[1] : "");
                }
            }
        }
        return cookieMap;
    }

    /**
     * Constructs a set-cookie header from the provided name, value, and arguments
     * @param name - cookie name
     * @param value - cookie value
     * @param args - any other cookie arguments such as expiration, site origins, etc
     * @return - the completed cookie in string form
     */
    public static String buildCookie(String name, String value, String... args) {
        String cookie = "Set-Cookie: " + name + "=" + value;
        for (int i = 0; i < args.length; i++) {
            cookie += "; " + args[i];
        }
        return cookie;
    }


    /**
     * Checks if provided string only contains numbers
     * @param s - String to check
     * @return - true if string only contains numbers, false otherwise
     */
    public static boolean isNumeric(String s) {
        if (s == null)
            return false;
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
