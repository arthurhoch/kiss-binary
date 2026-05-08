package io.github.arthurhoch.kissbinary.rinha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

final class RinhaJsonLineParser {

    private RinhaJsonLineParser() {}

    interface RecordConsumer {
        void accept(double[] vector, int label);
    }

    static long parseRecords(InputStream gzipInput, RecordConsumer consumer) throws IOException {
        long count = 0;
        try (InputStream gzip = new GZIPInputStream(gzipInput);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8))) {

            StringBuilder objectBuilder = new StringBuilder(256);
            int braceDepth = 0;
            boolean inString = false;
            boolean escaped = false;
            int ch;

            while ((ch = reader.read()) != -1) {
                char c = (char) ch;

                if (escaped) {
                    if (braceDepth > 0) objectBuilder.append(c);
                    escaped = false;
                    continue;
                }

                if (c == '\\' && inString) {
                    if (braceDepth > 0) objectBuilder.append(c);
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inString = !inString;
                    if (braceDepth > 0) objectBuilder.append(c);
                    continue;
                }

                if (inString) {
                    if (braceDepth > 0) objectBuilder.append(c);
                    continue;
                }

                if (c == '{') {
                    if (braceDepth > 0) objectBuilder.append(c);
                    braceDepth++;
                    if (braceDepth == 1) {
                        objectBuilder.setLength(0);
                        objectBuilder.append(c);
                    }
                } else if (c == '}') {
                    if (braceDepth > 0) objectBuilder.append(c);
                    braceDepth--;
                    if (braceDepth == 0 && objectBuilder.length() > 0) {
                        String json = objectBuilder.toString();
                        objectBuilder.setLength(0);
                        double[] vector = extractVector(json);
                        if (vector != null) {
                            int label = extractLabel(json);
                            consumer.accept(vector, label);
                            count++;
                        }
                    }
                } else {
                    if (braceDepth > 0) objectBuilder.append(c);
                }
            }
        }
        return count;
    }

    static double[] extractVector(String json) {
        String key = "\"vector\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return null;

        int openBracket = json.indexOf('[', keyIdx + key.length());
        if (openBracket < 0) return null;

        int closeBracket = json.indexOf(']', openBracket);
        if (closeBracket < 0) return null;

        String arrayContent = json.substring(openBracket + 1, closeBracket);
        String[] parts = arrayContent.split(",");

        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }

    static int extractLabel(String json) {
        String key = "\"label\"";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return 0;

        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) return 0;

        int start = colonIdx + 1;
        while (start < json.length() && json.charAt(start) != '"') {
            start++;
        }
        if (start >= json.length()) return 0;

        int end = json.indexOf('"', start + 1);
        if (end < 0) return 0;

        String labelStr = json.substring(start + 1, end);
        if ("fraud".equals(labelStr)) return 1;
        return 0;
    }
}
