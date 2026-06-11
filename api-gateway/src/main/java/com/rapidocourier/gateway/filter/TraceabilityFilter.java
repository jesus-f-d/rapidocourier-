package com.rapidocourier.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
public class TraceabilityFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceabilityFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Request-Id", requestId)
                .header("X-Gateway-Timestamp", timestamp)
                .build();

        log.info("Gateway request: id={} method={} path={} time={}",
                requestId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                timestamp);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() ->
                        log.info("Gateway response: id={} status={}",
                                requestId,
                                exchange.getResponse().getStatusCode())));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
