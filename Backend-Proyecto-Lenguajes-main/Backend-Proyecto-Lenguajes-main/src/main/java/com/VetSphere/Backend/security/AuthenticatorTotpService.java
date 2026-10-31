package com.VetSphere.Backend.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/** TOTP compatible con Google Authenticator, Authy y otras apps RFC 6238. */
@Service
public class AuthenticatorTotpService {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final long STEP_SECONDS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String nuevoSecreto() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return codificarBase32(bytes);
    }

    public String secretoDesdeClave(String clave) {
        try { return codificarBase32(MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    public boolean valido(String secreto, String codigo) {
        if (secreto == null || secreto.isBlank() || codigo == null || !codigo.matches("\\d{6}")) return false;
        long pasoActual = System.currentTimeMillis() / 1000 / STEP_SECONDS;
        for (long desfase = -1; desfase <= 1; desfase++) {
            if (java.security.MessageDigest.isEqual(codigo.getBytes(StandardCharsets.US_ASCII),
                    codigo(secreto, pasoActual + desfase).getBytes(StandardCharsets.US_ASCII))) return true;
        }
        return false;
    }

    public String codigoActual(String secreto) {
        return codigo(secreto, System.currentTimeMillis() / 1000 / STEP_SECONDS);
    }

    public int segundosRestantes() {
        return (int) (STEP_SECONDS - (System.currentTimeMillis() / 1000 % STEP_SECONDS));
    }

    public String uri(String correo, String secreto) {
        String cuenta = "VetSphere:" + correo;
        return "otpauth://totp/" + codificarUrl(cuenta) + "?secret=" + secreto
                + "&issuer=VetSphere&algorithm=SHA1&digits=6&period=30";
    }

    public String qrDataUri(String texto) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new MultiFormatWriter().encode(texto, BarcodeFormat.QR_CODE, 300, 300, hints);
            BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 300; y++) for (int x = 0; x < 300; x++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x102033 : 0xffffff);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el QR del autenticador", e);
        }
    }

    private String codigo(String secreto, long paso) {
        try {
            byte[] key = decodificarBase32(secreto);
            byte[] mensaje = new byte[8];
            for (int i = 7; i >= 0; i--) { mensaje[i] = (byte) (paso & 0xff); paso >>>= 8; }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(mensaje);
            int offset = hash[hash.length - 1] & 0x0f;
            int binario = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format("%06d", binario % 1_000_000);
        } catch (Exception e) {
            throw new IllegalArgumentException("Secreto TOTP no válido", e);
        }
    }

    private byte[] decodificarBase32(String texto) {
        String limpio = texto.replace("=", "").replaceAll("\\s", "").toUpperCase();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int buffer = 0, bits = 0;
        for (char c : limpio.toCharArray()) {
            int valor = BASE32.indexOf(c);
            if (valor < 0) throw new IllegalArgumentException("Base32 inválido");
            buffer = (buffer << 5) | valor;
            bits += 5;
            if (bits >= 8) { salida.write((buffer >> (bits -= 8)) & 0xff); }
        }
        return salida.toByteArray();
    }

    private String codificarBase32(byte[] bytes) {
        StringBuilder salida = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) salida.append(BASE32.charAt((buffer >> (bits -= 5)) & 31));
        }
        if (bits > 0) salida.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return salida.toString();
    }

    private String codificarUrl(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
