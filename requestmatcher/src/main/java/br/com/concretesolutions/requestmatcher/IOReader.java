package br.com.concretesolutions.requestmatcher;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

final class IOReader {

    static String read(InputStream is) {

        if (is == null) {
            throw new IllegalArgumentException("Could not open resource stream.");
        }

        final BufferedReader bis = new BufferedReader(new InputStreamReader(is));
        final StringBuilder builder = new StringBuilder();

        String line;
        try {
            while ((line = bis.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource fully", e);
        } finally {

            try {
                is.close();
            } catch (IOException e) {
                Log.e(IOReader.class.getSimpleName(), "Error while trying to close stream", e);
                // do nothing here
            }
        }

        return builder.append("\n").toString();
    }

    static byte[] readBinary(InputStream is) {

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();

            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource fully", e);
        } finally {

            try {
                is.close();
            } catch (IOException e) {
                Log.e(IOReader.class.getSimpleName(), "Error while trying to close stream", e);
                // do nothing here
            }
        }
    }

    /*
    This is not using MimeTypeMap or URLConnection to guess the mime-type by the path extension
    because Robolectric does not implement it. That would never find the mime-type for local tests.
     */

    static String mimeTypeFromExtension(String path) {

        if (TextUtils.isEmpty(path)) {
            return null;
        }

        final int indexOfDot = path.lastIndexOf('.');

        if (indexOfDot == -1) {
            return null;
        }

        final String ext = path.substring(indexOfDot + 1)
                .toLowerCase(Locale.getDefault());

        switch (ext) {
            case "json":
                return "application/json";
            case "xml":
                return "text/xml";
            case "txt":
                return "text/plain";
            case "html":
                return "text/html";
            case "js":
                return "text/javascript";
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "png":
                return "image/png";
            case "jpg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            default:
                return null;
        }
    }

    private IOReader() {}
}
