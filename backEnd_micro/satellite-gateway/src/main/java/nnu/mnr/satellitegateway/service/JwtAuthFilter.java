package nnu.mnr.satellitegateway.service;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import nnu.mnr.satellitegateway.utils.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/21 10:42
 * @Description:
 */

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/v1/user/login") || path.startsWith("/api/v1/user/register") || path.startsWith("/api/v1/user/refresh")) {
            return chain.filter(exchange);
        }
        String accessToken = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
            try {
                if (JwtUtil.isTokenExpired(accessToken)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                Claims claims = JwtUtil.parseToken(accessToken);
                String userId = claims.getSubject();
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (Exception e) {
                log.error("Error JWT Validation : {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } else {
            log.warn("Wrong Authorization");
            exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

}
