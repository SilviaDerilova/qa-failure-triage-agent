package com.silvia.qa.support.evidence;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/** Keeps the most recent HTTP exchange for a single test. */
public final class ApiEvidenceFilter implements Filter {
    String httpMethod;
    String requestUrl;
    Integer responseStatusCode;
    String responseBody;
    boolean responseBodyTruncated;
    Long responseTimeMs;

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        reset();
        httpMethod = request.getMethod();
        requestUrl = request.getURI();
        Response response = context.next(request, responseSpec);
        responseStatusCode = response.statusCode();
        responseBody = response.asString();
        responseBodyTruncated = responseBody.length() > 3000;
        if (responseBodyTruncated) {
            responseBody = responseBody.substring(0, 3000);
        }
        responseTimeMs = response.time();
        return response;
    }

    void reset() {
        httpMethod = null;
        requestUrl = null;
        responseStatusCode = null;
        responseBody = null;
        responseBodyTruncated = false;
        responseTimeMs = null;
    }
}
