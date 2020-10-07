package com.cpt22.HTTPServer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileParser {
    private static final String[] DELIM = {"%", "%"};

    public static String parseFile(File file, Map<String, String> symbolMap) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> lines = new ArrayList<String>();
        String line = null;
        String allLines = "";
        try {

            line = reader.readLine();
            while (line != null) {
                allLines += line;
                line = reader.readLine();
            }

                String[] lineSplit = allLines.split("");
                for (int i = 0; i < lineSplit.length - 1; i++) {
                    int index1, index2 = -1;
                    String symbol = "";
                    if (lineSplit[i].equals(DELIM[0]) && lineSplit[i+1].equals(DELIM[1])) {
                        index1 = i;
                        for (int j = i+2; j < lineSplit.length - 2; j++) {
                            if (lineSplit[j].equals(DELIM[0]) && lineSplit[j+1].equals(DELIM[1])) {
                                index2 = j;
                                i = j+2;
                                break;
                            }
                            symbol += lineSplit[j];
                        }

                        if (symbolMap.containsKey(symbol) && index1 != -1 && index2 != -1) {
                            allLines = allLines.replaceAll(DELIM[0] + DELIM[1] + symbol + DELIM[0] + DELIM[1], symbolMap.get(symbol));
                        } else {
                            allLines = allLines.replaceAll(DELIM[0] + DELIM[1] + symbol + DELIM[0] + DELIM[1], "");
                        }
                    }
                }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allLines;
    }
}
