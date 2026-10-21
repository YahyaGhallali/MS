package com.example.productservice;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InstanceIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(InstanceIdFilter.class);

    @Value("${server.port:8081}")
    private String serverPort;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String instanceId = System.getenv("HOSTNAME");
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = "product-service-port-" + serverPort;
        }

        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("X-Instance-Id", instanceId);
        }

        chain.doFilter(request, response);
    }
}
