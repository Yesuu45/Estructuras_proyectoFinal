package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Usuario;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import co.edu.uniquindio.poo.servicio.RedTransferencias;
import co.edu.uniquindio.poo.servicio.ResumenNivel;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reportes del administrador: ranking y consultas por puntos (árbol) y red de transferencias (grafo). */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final PlataformaService servicio;

    public ReporteController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/ranking")
    public Usuario[] ranking(HttpSession sesion) {
        return servicio.rankingDePuntos(SesionActual.identificacion(sesion));
    }

    @GetMapping("/niveles")
    public ResumenNivel[] niveles(HttpSession sesion) {
        return servicio.resumenPorNiveles(SesionActual.identificacion(sesion));
    }

    @GetMapping("/usuarios-por-puntos")
    public Usuario[] usuariosPorPuntos(@RequestParam int minimo, @RequestParam int maximo, HttpSession sesion) {
        return servicio.usuariosPorRango(SesionActual.identificacion(sesion), minimo, maximo);
    }

    @GetMapping("/red-usuarios")
    public RedTransferencias redUsuarios(HttpSession sesion) {
        return servicio.redDeUsuarios(SesionActual.identificacion(sesion));
    }

    @GetMapping("/red-billeteras")
    public RedTransferencias redBilleteras(HttpSession sesion) {
        return servicio.redDeBilleteras(SesionActual.identificacion(sesion));
    }

    @GetMapping("/ruta")
    public String[] ruta(@RequestParam String desde, @RequestParam String hasta, HttpSession sesion) {
        return servicio.rutaDeDinero(SesionActual.identificacion(sesion), desde, hasta);
    }
}
