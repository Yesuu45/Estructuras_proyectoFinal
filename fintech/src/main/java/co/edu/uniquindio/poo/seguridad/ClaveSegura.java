package co.edu.uniquindio.poo.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Guarda contraseñas sin texto plano: PBKDF2 con sal aleatoria por usuario.
 * El resultado tiene la forma "sal$hash" (ambos en Base64) y cabe en VARCHAR(128).
 */
public final class ClaveSegura {

    public static final int LONGITUD_MINIMA = 6;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private ClaveSegura() {
    }

    public static String generarHash(String clave) {
        if (clave == null || clave.length() < LONGITUD_MINIMA) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(clave, sal);
        return Base64.getEncoder().encodeToString(sal) + "$" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean coincide(String clave, String almacenado) {
        if (clave == null || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split("\\$");
        if (partes.length != 2) {
            return false;
        }
        try {
            byte[] sal = Base64.getDecoder().decode(partes[0]);
            byte[] esperado = Base64.getDecoder().decode(partes[1]);
            // Comparación en tiempo constante, para no filtrar información por el tiempo de respuesta
            return MessageDigest.isEqual(esperado, derivar(clave, sal));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String clave, byte[] sal) {
        PBEKeySpec especificacion = new PBEKeySpec(clave.toCharArray(), sal, ITERACIONES, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
