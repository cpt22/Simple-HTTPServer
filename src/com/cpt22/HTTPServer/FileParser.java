package com.cpt22.HTTPServer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileParser {
    private static final String[] DELIM = {"%", "%"};

    /**
     * Parses the specified file and replaces the symbols within the delimiters with the specified value in the map
     * @param file - The file to parse
     * @param symbolMap - The map of symbols and values to replace in the file
     * @return - The file in string format with the proper replacements
     * @throws FileNotFoundException
     */
    public static String parseFile(File file, Map<String, Object> symbolMap) throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> lines = new ArrayList<String>();
        String line = null;
        String allLines = "";
        try {
            // Read all of the file lines into a single string
            line = reader.readLine();
            while (line != null) {
                allLines += line;
                line = reader.readLine();
            }

            // Split the lines into an array of strings 1 character long
            String[] lineSplit = allLines.split("");
            // Loop through all characters searching for the delimiters
            for (int i = 0; i < lineSplit.length - 1; i++) {
                int index1, index2 = -1;
                String symbol = "";
                // If the two delimiter characters are found in order, attempt to obtain the symbol
                if (lineSplit[i].equals(DELIM[0]) && lineSplit[i+1].equals(DELIM[1])) {
                    index1 = i;
                    // Loop through the characters between the sets of delimiters and build the symbol
                    for (int j = i+2; j < lineSplit.length - 2; j++) {
                        if (lineSplit[j].equals(DELIM[0]) && lineSplit[j+1].equals(DELIM[1])) {
                            index2 = j;
                            i = j+2;
                            break;
                        }
                        symbol += lineSplit[j];
                    }

                    // Replace all of the found symbol with its associated value in the map, or with the empty String if it is not found in the map
                    if (symbolMap.containsKey(symbol) && index1 != -1 && index2 != -1) {
                        allLines = allLines.replaceAll(DELIM[0] + DELIM[1] + symbol + DELIM[0] + DELIM[1], String.valueOf(symbolMap.get(symbol)));
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
