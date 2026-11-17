package co.edu.uniquindio.poo.modelo;

import co.edu.uniquindio.poo.Estructuras.SimpleLinkedList;

import java.math.BigDecimal;

public class Billetera {

    private final String codigo;
    private final String nombre;
    private final TipoBilletera tipo;
    private final String identificacionTitular;
    private BigDecimal saldo;
    private BigDecimal umbralSaldoBajo;
    private boolean activa;
    // Lista enlazada con los movimientos de esta billetera, en orden de llegada
    private final SimpleLinkedList<Transaccion> historial = new SimpleLinkedList<>();

    public Billetera(String codigo, String nombre, TipoBilletera tipo,
                     String identificacionTitular, BigDecimal saldoInicial,
                     BigDecimal umbralSaldoBajo) {
        this.codigo = validarTexto(codigo, "El código");
        this.nombre = validarTexto(nombre, "El nombre");
        this.tipo = java.util.Objects.requireNonNull(tipo, "El tipo es obligatorio");
        this.identificacionTitular = validarTexto(identificacionTitular, "La identificación del titular");
        this.saldo = saldoInicial == null ? BigDecimal.ZERO : saldoInicial;
        this.umbralSaldoBajo = umbralSaldoBajo == null ? BigDecimal.ZERO : umbralSaldoBajo;
        if (this.saldo.signum() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo");
        }
        this.activa = true;
    }

    public void recargar(BigDecimal monto) {
        validarMonto(monto);
        asegurarActiva();
        saldo = saldo.add(monto);
    }

    public void retirar(BigDecimal monto) {
        validarMonto(monto);
        asegurarActiva();
        if (monto.compareTo(saldo) > 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        saldo = saldo.subtract(monto);
    }

    public void registrarMovimiento(Transaccion transaccion) {
        historial.addLast(transaccion);
    }

    // Sin prefijo get: así Jackson no incluye la lista en el JSON de la billetera
    public SimpleLinkedList<Transaccion> historialMovimientos() {
        return historial;
    }

    public boolean tieneSaldoBajo() {
        return saldo.compareTo(umbralSaldoBajo) < 0;
    }

    public void bloquear() {
        activa = false;
    }

    public void activar() {
        activa = true;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoBilletera getTipo() {
        return tipo;
    }

    public String getIdentificacionTitular() {
        return identificacionTitular;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getUmbralSaldoBajo() {
        return umbralSaldoBajo;
    }

    public void setUmbralSaldoBajo(BigDecimal umbral) {
        if (umbral == null || umbral.signum() < 0) {
            throw new IllegalArgumentException("El umbral no puede ser negativo");
        }
        this.umbralSaldoBajo = umbral;
    }

    public boolean isActiva() {
        return activa;
    }

    private void asegurarActiva() {
        if (!activa) {
            throw new IllegalStateException("La billetera " + codigo + " está bloqueada");
        }
    }

    private static void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor.trim();
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) return true;
        if (!(otro instanceof Billetera)) return false;
        Billetera billetera = (Billetera) otro;
        return codigo.equals(billetera.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

    @Override
    public String toString() {
        return codigo + " (" + tipo + ") saldo: " + saldo;
    }
}