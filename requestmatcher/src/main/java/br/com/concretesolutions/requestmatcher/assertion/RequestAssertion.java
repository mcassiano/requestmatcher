package br.com.concretesolutions.requestmatcher.assertion;

import okhttp3.mockwebserver.RecordedRequest;

public interface RequestAssertion {
    void doAssert(RecordedRequest request);
}
