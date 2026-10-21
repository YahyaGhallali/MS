package com.example.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoggingAndCorrelationFilterTest {

    private final LoggingAndCorrelationFilter filter = new LoggingAndCorrelationFilter();

    @Test
    @DisplayName("Should generate and attach X-Correlation-Id header when not present in request")
    void testFilterGeneratesCorrelationId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/products").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // Verify request was mutated with correlation ID
        verify(chain).filter(argThat(mutatedExchange -> {
            String correlationId = mutatedExchange.getRequest().getHeaders().getFirst(LoggingAndCorrelationFilter.CORRELATION_ID_HEADER);
            return correlationId != null && !correlationId.isBlank();
        }));

        // Verify response header contains correlation ID
        String responseCorrelationId = exchange.getResponse().getHeaders().getFirst(LoggingAndCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(responseCorrelationId).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Should preserve existing X-Correlation-Id header if already provided")
    void testFilterPreservesExistingCorrelationId() {
        String existingId = "custom-trace-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/orders")
                .header(LoggingAndCorrelationFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // Verify response header contains the same correlation ID
        String responseCorrelationId = exchange.getResponse().getHeaders().getFirst(LoggingAndCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(responseCorrelationId).isEqualTo(existingId);
    }

    @Test
    @DisplayName("Filter order should be -1 for high precedence")
    void testGetOrder() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}
