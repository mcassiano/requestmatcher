package br.com.concretesolutions.requestmatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Utility class for preparing parameters to be matched against.
 */
public final class RequestUtils {

    public static Map<String, String> buildQueryMap(String path) {
        final Map<String, String> queries = new HashMap<>();
        final String queryString = path.substring(path.indexOf('?') + 1);
        final String[] queryParts = queryString.split("&");

        if (queryParts.length == 0)
            return queries;

        for (String part : queryParts) {
            final String[] split = part.split("=");
            queries.put(split[0], split[1]);
        }

        return queries;
    }

    public static Map<String, String> buildHeadersMap(Headers headers) {

        final Map<String, List<String>> stringListMap = headers.toMultimap();
        final Map<String, String> headersMap = new HashMap<>(headers.size());

        final StringBuilder headerValue = new StringBuilder();

        for (String key : stringListMap.keySet()) {

            final List<String> values = stringListMap.get(key);

            for (int i = 0; i < values.size(); i++) {

                if (i > 0)
                    headerValue.append(';');

                headerValue.append(values.get(i));
            }

            headersMap.put(key, headerValue.toString());
            headerValue.setLength(0);
        }

        return headersMap;
    }

    public static String getPathOnly(RecordedRequest request) {

        final String path = request.getPath();

        if (!path.contains("?"))
            return path;

        return path.substring(0, path.indexOf('?'));
    }
}
