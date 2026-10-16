package com.liceo.campustrack.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.liceo.campustrack.model.ClassSession;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QR images plus the rotating session codes.
 *
 * Time is cut into 20-second windows. The code shown for a window is signed with the
 * session's secret (HMAC-SHA256), so a screenshot stops working once its window passes
 * and nobody can forge the next code without the secret.
 */
@Service
public class QrCodeService {

    public static final int ROTATE_SECONDS = 20;

    private static final Pattern FULL_CODE = Pattern.compile("^CT1\\.(\\d{1,12})\\.(\\d{1,12})\\.([A-Za-z0-9_-]{16})$");
    private static final Pattern MANUAL_CODE = Pattern.compile("^(\\d{1,12})-(\\d{6})$");

    /** A scanned or typed code. {@code window} is null for the short manual code. */
    public record Token(long sessionId, Long window, String proof) {}

    public String toPngBase64(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", png);
            return Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not generate the QR code", e);
        }
    }

    /** @return the text inside the QR image, or null when no QR code can be found */
    public String decodeImage(InputStream image) throws IOException {
        BufferedImage picture = ImageIO.read(image);
        if (picture == null) return null;
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(picture)));
            return new MultiFormatReader().decode(bitmap).getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    public long currentWindow() {
        return Instant.now().getEpochSecond() / ROTATE_SECONDS;
    }

    public int secondsLeftInWindow() {
        return ROTATE_SECONDS - (int) (Instant.now().getEpochSecond() % ROTATE_SECONDS);
    }

    public LocalDateTime windowStart(long window) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(window * ROTATE_SECONDS), ZoneId.systemDefault());
    }

    /** Text encoded in the projected QR code for the given window. */
    public String sessionPayload(ClassSession session, long window) {
        String proof = base64(hmac(session.getSecret(), "qr:" + session.getId() + ":" + window)).substring(0, 16);
        return "CT1." + session.getId() + "." + window + "." + proof;
    }

    /** Short code a student can type when their camera is broken, e.g. "12-048213". */
    public String manualCode(ClassSession session, long window) {
        byte[] mac = hmac(session.getSecret(), "code:" + session.getId() + ":" + window);
        int number = ((mac[0] & 0x7f) << 24) | ((mac[1] & 0xff) << 16) | ((mac[2] & 0xff) << 8) | (mac[3] & 0xff);
        return session.getId() + "-" + String.format("%06d", number % 1_000_000);
    }

    public Token parse(String payload) {
        if (payload == null) return null;
        String text = payload.trim();
        Matcher full = FULL_CODE.matcher(text);
        if (full.matches()) {
            return new Token(Long.parseLong(full.group(1)), Long.parseLong(full.group(2)), full.group(3));
        }
        Matcher manual = MANUAL_CODE.matcher(text);
        if (manual.matches()) {
            return new Token(Long.parseLong(manual.group(1)), null, manual.group(2));
        }
        return null;
    }

    /** True when the token is exactly the code that was valid during {@code window}. */
    public boolean matches(ClassSession session, Token token, long window) {
        String expected;
        if (token.window() == null) {
            expected = manualCode(session, window);
            expected = expected.substring(expected.indexOf('-') + 1);
        } else {
            if (token.window() != window) return false;
            String payload = sessionPayload(session, window);
            expected = payload.substring(payload.lastIndexOf('.') + 1);
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                token.proof().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
