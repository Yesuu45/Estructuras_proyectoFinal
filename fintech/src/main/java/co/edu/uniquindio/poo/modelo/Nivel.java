package co.edu.uniquindio.poo.modelo;

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
