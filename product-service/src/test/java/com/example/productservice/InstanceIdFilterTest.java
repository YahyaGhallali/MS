package com.example.productservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceIdFilterTest {

    @Test
    @DisplayName("InstanceIdFilter should attach X-Instance-Id header to HTTP responses")
    void testFilterAddsInstanceHeader() throws Exception {
        InstanceIdFilter filter = new InstanceIdFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String instanceId = response.getHeader("X-Instance-Id");
        assertThat(instanceId).isNotNull().isNotBlank();
    }
}
