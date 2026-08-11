package com.wallet.auth.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * CORS(교차 출처 리소스 공유) 필터.
 *
 * 응답 래퍼를 사용해 모든 응답에 CORS 헤더를 확실히 추가!
 * 프론트(localhost:5173)와 백엔드(localhost:8080)가 다른 포트에 있으니,
 * 브라우저의 CORS 정책을 우회하기 위해 필요
 *
 * 응답이 커밋된 후에도 헤더가 유지되도록 보장한다.
 */
public class CorsFilter implements Filter {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String OPTIONS_METHOD = "OPTIONS";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");

        // Preflight 요청(OPTIONS)은 여기서 처리하고 반환한다.
        if (OPTIONS_METHOD.equalsIgnoreCase(httpRequest.getMethod())) {
            if (ALLOWED_ORIGIN.equals(origin)) {
                httpResponse.setHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
                httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
                // Authorization 헤더를 명시적으로 포함해야 브라우저가 인식한다.
                httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, *");
                httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
                httpResponse.setHeader("Vary", "Origin");
            }
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 실제 요청의 응답에 CORS 헤더를 확실히 추가하기 위해 응답을 래핑한다.
        // 래퍼는 응답이 커밋되기 직전에 CORS 헤더를 추가한다.
        HttpServletResponse wrappedResponse = new HttpServletResponseWrapper(httpResponse) {
            private boolean headerAdded = false;

            @Override
            public void flushBuffer() throws IOException {
                addCorsHeaderIfNeeded();
                super.flushBuffer();
            }

            @Override
            public void sendError(int sc) throws IOException {
                addCorsHeaderIfNeeded();
                super.sendError(sc);
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {
                addCorsHeaderIfNeeded();
                super.sendError(sc, msg);
            }

            @Override
            public void sendRedirect(String location) throws IOException {
                addCorsHeaderIfNeeded();
                super.sendRedirect(location);
            }

            private void addCorsHeaderIfNeeded() {
                if (!headerAdded && ALLOWED_ORIGIN.equals(httpRequest.getHeader("Origin"))) {
                    if (!containsHeader("Access-Control-Allow-Origin")) {
                        addHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
                        addHeader("Access-Control-Allow-Credentials", "true");
                        addHeader("Vary", "Origin");
                    }
                    headerAdded = true;
                }
            }
        };

        // 허용된 오리진에서 온 요청이면 CORS 헤더를 미리 설정
        if (ALLOWED_ORIGIN.equals(origin)) {
            wrappedResponse.setHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
            wrappedResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            // Authorization 헤더를 명시적으로 포함해야 브라우저가 인식
            wrappedResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, *");
            wrappedResponse.setHeader("Access-Control-Allow-Credentials", "true");
            wrappedResponse.setHeader("Vary", "Origin");
        }

        chain.doFilter(request, wrappedResponse);
    }

    @Override
    public void destroy() {}
}
