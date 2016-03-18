package br.com.concretesolutions.requestmatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

final class IOReader {

    static String read(InputStream is) {

        if (is == null)
            throw new IllegalArgumentException("Could not open resource stream.");

        final BufferedReader bis = new BufferedReader(new InputStreamReader(is));
        final StringBuilder builder = new StringBuilder();

        String line;
        try {
            while ((line = bis.readLine()) != null)
                builder.append(line);
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource fully", e);
        } finally {

            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                // do nothing here
            }
        }

        return builder.append("\n").toString();
    }
}
