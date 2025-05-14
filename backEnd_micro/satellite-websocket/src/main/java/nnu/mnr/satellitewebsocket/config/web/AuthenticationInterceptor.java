package nnu.mnr.satellitewebsocket.config.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nnu.mnr.satellitewebsocket.client.UserClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2025/4/21 17:17
 * @Description:
 */

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final ObjectProvider<UserClient> userClientProvider;

    public AuthenticationInterceptor(ObjectProvider<UserClient> userClientProvider) {
        this.userClientProvider = userClientProvider;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, Object handler) throws Exception {
        try {
            String internalHeader = request.getHeader("X-Internal-Request");
            if ("true".equalsIgnoreCase(internalHeader)) {
                return true;
            }
            UserClient userClient = userClientProvider.getIfAvailable();
            String userId = request.getHeader("X-User-Id");
            Boolean isValid = userClient.validateUser(userId);
            if (!Boolean.TRUE.equals(isValid)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("User not found or inactive");
                return false;
            }

            request.setAttribute("authenticatedUsername", userId);
            return true;

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token validation failed");
            return false;
        }
    }

}
