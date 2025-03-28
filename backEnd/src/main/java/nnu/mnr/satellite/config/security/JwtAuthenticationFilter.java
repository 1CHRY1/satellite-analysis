//package nnu.mnr.satellite.config.security;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import nnu.mnr.satellite.utils.security.JwtUtil;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
///**
// * Created with IntelliJ IDEA.
// *
// * @Author: Chry
// * @Date: 2025/3/28 16:56
// * @Description:
// */
//
//@Component
//@Slf4j
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final UserDetailsService userDetailsService;
//
//    public JwtAuthenticationFilter(UserDetailsService userDetailsService) {
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//        String accessToken = request.getHeader("Authorization");
//        String refreshToken = request.getHeader("Refresh-Token");
//
//        if (accessToken != null && !accessToken.isEmpty()) {
//            try {
//                if (JwtUtil.isTokenExpired(accessToken)) {
//                    if (refreshToken == null || JwtUtil.isTokenExpired(refreshToken)) {
//                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Tokens expired");
//                        return;
//                    }
//                    String userId = JwtUtil.getUserIdFromToken(refreshToken);
//                    UserDetails userDetails = userDetailsService.loadUserByUsername(JwtUtil.getUsernameFromToken(refreshToken));
//                    String newAccessToken = JwtUtil.generateAccessToken(userId, userDetails.getUsername(), JwtUtil.getRoleFromToken(refreshToken));
//                    response.setHeader("New-Access-Token", newAccessToken);
//                    log.info("Access token refreshed for user: {}", userId);
//                    accessToken = newAccessToken;
//                }
//
//                String username = JwtUtil.getUsernameFromToken(accessToken);
//                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//                UsernamePasswordAuthenticationToken authentication =
//                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//            } catch (Exception e) {
//                log.error("Token validation failed", e);
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
//                return;
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//}
