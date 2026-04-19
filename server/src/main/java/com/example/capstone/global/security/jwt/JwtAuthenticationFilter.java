package com.example.capstone.global.security.jwt;

import com.example.capstone.domain.auth.exception.AuthErrorCode;
import com.example.capstone.global.api.ApiError;
import com.example.capstone.global.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveBearerToken(request);

        if (token != null) {
            // 1) 토큰 유효성(서명/만료) 체크
            if (!jwtProvider.validateToken(token)) {
                writeUnauthorized(response, AuthErrorCode.AUTH_INVALID_TOKEN);
                return;
            }

            // 2) Access 토큰만 인증에 사용하도록 타입 체크
            String type = jwtProvider.getTokenType(token);
            if (!"ACCESS".equals(type)) {
                writeUnauthorized(response, AuthErrorCode.AUTH_ACCESS_TOKEN_REQUIRE);
                return;
            }

            // 3) userId 추출 후 인증 객체 생성
            Long userId = jwtProvider.getUserId(token);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            // principal에 userId를 담아두면 컨트롤러에서 꺼내기 쉬움
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header)) return null;
        if (!header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }

    private void writeUnauthorized(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = new ApiError(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );

        ApiResponse<Void> body = ApiResponse.fail(error);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
