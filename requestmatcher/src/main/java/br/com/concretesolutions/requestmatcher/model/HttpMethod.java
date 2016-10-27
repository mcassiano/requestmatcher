package br.com.concretesolutions.requestmatcher.model;

import okhttp3.mockwebserver.RecordedRequest;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH,
    OPTIONS,
    HEAD,
    COPY,
    MOVE;

    public static HttpMethod forRequest(RecordedRequest request) {

        final String method = request.getMethod();

        for (HttpMethod httpMethod : values()) {
            if (httpMethod.name().equals(method)) {
                return httpMethod;
            }
        }

        throw new IllegalArgumentException("Can't recognize method");
    }
}
