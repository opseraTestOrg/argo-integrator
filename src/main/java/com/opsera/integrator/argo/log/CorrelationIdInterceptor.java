package com.opsera.integrator.argo.log;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * The Class CorrelationIdInterceptor.
 */
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    /**
     * Intercept.
     *
     * @param request the request
     * @param body the body
     * @param execution the execution
     * @return the client http response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        headers.add("X-Correlation-Id", MDC.get("correlation_id"));
        return execution.execute(request, body);
    }

}
