package co.edu.uniquindio.poo.modelo;

import java.math.BigDecimal;

public enum Nivel {
    BRONCE(0,500),
    PLATA(501,1000),
    ORO(1001,5000),
    PLATINO(5001 , Integer.MAX_VALUE);

    private final int minimo;
    private final int maximo;


    Nivel(int minimo, int maximo) {
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public int getMinimo() {
        return minimo;
    }

    public int getMaximo() {
        return maximo;
    }

    /** Ventaja del nivel: valor máximo por operación de salida (retiro, transferencia o pago). null = sin límite. */
    public BigDecimal getLimiteOperacion() {
        return switch (this) {
            case BRONCE -> new BigDecimal("200000");
            case PLATA -> new BigDecimal("500000");
            case ORO -> new BigDecimal("2000000");
            case PLATINO -> null;
        };
    }

    public static Nivel desdePuntos(int puntos) {
        if(puntos < 0){
            throw new IllegalArgumentException("puntos negativo");
        }
        for (Nivel nivel : values()){
            if(puntos >= nivel.minimo && puntos <= nivel.maximo){
                return nivel;
            }
        }
        return PLATINO;
    }
}
