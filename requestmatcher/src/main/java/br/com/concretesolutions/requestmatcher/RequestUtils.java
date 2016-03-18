package br.com.concretesolutions.requestmatcher;

import android.util.Pair;

import java.util.HashSet;
import java.util.Set;

import okhttp3.mockwebserver.RecordedRequest;

public final class RequestUtils {

    public static boolean hasQuery(String path) {
        return path.contains("?");
    }

    public static String getBody(RecordedRequest request) {
        return request.getBody().readUtf8();
    }

    public static Set<Pair<String, String>> buildQueries(String path) {
        final Set<Pair<String, String>> queries = new HashSet<>();
        final String queryString = path.substring(path.indexOf('?'));
        final String[] queryParts = queryString.split("&");

        for (String part : queryParts) {
            final String[] split = part.split("=");
            queries.add(Pair.create(split[0], split[1]));
        }

        return queries;
    }
}
