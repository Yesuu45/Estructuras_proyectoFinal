package co.edu.uniquindio.poo.ui;

import jakarta.servlet.http.HttpSession;

/** Lee de la sesión quién está conectado. FiltroSesion ya garantiza que hay sesión en /api/**. */
final class SesionActual {

    private SesionActual() {
    }

    static String identificacion(HttpSession sesion) {
        return (String) sesion.getAttribute(AutenticacionController.ATRIBUTO_USUARIO);
    }
}
