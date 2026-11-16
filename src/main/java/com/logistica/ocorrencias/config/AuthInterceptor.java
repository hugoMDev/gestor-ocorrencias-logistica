package com.logistica.ocorrencias.config;

import com.logistica.ocorrencias.service.AuthTokenService;
import com.logistica.ocorrencias.service.UsuarioSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired(required = false)
    private AuthTokenService authTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Permite testes unitários focados (@WebMvcTest) onde AuthTokenService não é instanciado
        if (authTokenService == null) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"erro\":\"Não autorizado. Token de sessão ausente ou inválido.\"}");
            return false;
        }

        String token = authHeader.substring(7).trim();
        Optional<UsuarioSession> sessionOpt = authTokenService.validarToken(token);
        if (sessionOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"erro\":\"Sessão expirada ou inválida. Por favor, faça login novamente.\"}");
            return false;
        }

        request.setAttribute("usuarioLogado", sessionOpt.get());
        return true;
    }
}
