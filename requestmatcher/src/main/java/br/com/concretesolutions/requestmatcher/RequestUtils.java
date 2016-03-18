package br.com.concretesolutions.requestmatcher;

import java.util.HashSet;
import java.util.Set;

import br.com.concretesolutions.requestmatcher.model.Query;
import okhttp3.mockwebserver.RecordedRequest;

public final class RequestUtils {

    public static boolean hasQuery(String path) {
        return path.contains("?");
    }

    public static String getBody(RecordedRequest request) {
        return request.getBody().readUtf8();
    }

    public static Set<Query> buildQueries(String path) {
        final Set<Query> queries = new HashSet<>();
        final String queryString = path.substring(path.indexOf('?') + 1);
        final String[] queryParts = queryString.split("&");

        for (String part : queryParts) {
            final String[] split = part.split("=");
            queries.add(Query.of(split[0], split[1]));
        }

        return queries;
    }

    public static String getPathOnly(RecordedRequest request) {

        final String path = request.getPath();

        if (!hasQuery(path))
            return path;

        return path.substring(0, path.indexOf('?'));
    }
}
