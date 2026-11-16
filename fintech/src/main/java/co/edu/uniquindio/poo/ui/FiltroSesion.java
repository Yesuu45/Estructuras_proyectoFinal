package co.edu.uniquindio.poo.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * Deja pasar a /api/** solo a quien tenga sesión iniciada.
 * Públicas: /api/auth/** (login, logout, quién soy) y el registro de usuarios (POST /api/usuarios).
 */
public class FiltroSesion implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador)
            throws Exception {
        String ruta = peticion.getRequestURI();
        if (ruta.startsWith("/api/auth/")
                || ("POST".equals(peticion.getMethod()) && "/api/usuarios".equals(ruta))) {
            return true;
        }
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null && sesion.getAttribute(AutenticacionController.ATRIBUTO_USUARIO) != null) {
            return true;
        }
        respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("{\"mensaje\":\"Debes iniciar sesión\"}");
        return false;
    }
}
