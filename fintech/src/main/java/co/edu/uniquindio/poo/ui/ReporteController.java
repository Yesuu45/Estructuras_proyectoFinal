package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Usuario;
import co.edu.uniquindio.poo.servicio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

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

    // ---------- Analítica (4.8) ----------

    @GetMapping("/analitica/billeteras-usadas")
    public Conteo[] billeterasUsadas(@RequestParam(defaultValue = "5") int n, HttpSession sesion) {
        return servicio.billeterasMasUsadas(SesionActual.identificacion(sesion), n);
    }

    @GetMapping("/analitica/usuarios-transferencias")
    public Conteo[] usuariosTransferencias(@RequestParam(defaultValue = "5") int n, HttpSession sesion) {
        return servicio.usuariosConMasTransferencias(SesionActual.identificacion(sesion), n);
    }

    @GetMapping("/analitica/categorias")
    public Conteo[] categorias(HttpSession sesion) {
        return servicio.categoriasMasActivas(SesionActual.identificacion(sesion));
    }

    @GetMapping("/analitica/frecuencia-tipos")
    public Conteo[] frecuenciaTipos(HttpSession sesion) {
        return servicio.frecuenciaPorTipo(SesionActual.identificacion(sesion));
    }

    @GetMapping("/analitica/pares-frecuentes")
    public AristaRed[] paresFrecuentes(@RequestParam(defaultValue = "5") int n, HttpSession sesion) {
        return servicio.paresFrecuentes(SesionActual.identificacion(sesion), n);
    }

    /** desde y hasta: fecha (2026-11-16) o fecha y hora (2026-11-16T10:30). Sin hora, 'hasta' incluye todo el día. */
    @GetMapping("/analitica/monto")
    public MontoMovilizado monto(@RequestParam String desde, @RequestParam String hasta, HttpSession sesion) {
        return servicio.montoMovilizado(SesionActual.identificacion(sesion),
                leerFecha(desde, false), leerFecha(hasta, true));
    }

    private static LocalDateTime leerFecha(String texto, boolean finDeDia) {
        try {
            String limpio = texto == null ? "" : texto.trim();
            if (limpio.length() == 10) {
                LocalDate dia = LocalDate.parse(limpio);
                return finDeDia ? dia.atTime(23, 59, 59) : dia.atStartOfDay();
            }
            return LocalDateTime.parse(limpio);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha no válida: " + texto);
        }
    }

    // ---------- Ventajas por nivel (4.5) ----------

    @GetMapping("/ventajas")
    public VentajaNivel[] ventajas() {
        return servicio.ventajasPorNivel();
    }
}
