package com.cpt22.HTTPServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utility {

    public static String buildHeaders(ClientConnection cc, ResponseCode rc, List<String> extraHeaders, String mimeType, int contentLength) {
        String ret = "";
        ret += "HTTP/1.1 " + rc.code + " " + rc.text + "\n";
        ret += "Server: Christian Tingles HTTP Server v" + cc.getServer().getVersion() + "\n";
        ret += "Date: " + new Date() + "\n";
        ret += "Content-type: " + mimeType + "\n";
        ret += "Content-length: " + contentLength + "\n";
        if (!cc.getServer().areConnectionsPersistent())
            ret += "Connection: close\n";
        if (extraHeaders != null) {
            for (int i = 0; i < extraHeaders.size(); i++) {
                ret += extraHeaders.get(i) + "\n";
            }
        }
        ret += "\n";
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

    public static byte[] stringToByteArray(String str) throws IOException {
        return str.getBytes("UTF-8");
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
                    cookieMap.put(cookie[0], cookie[1]);
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
