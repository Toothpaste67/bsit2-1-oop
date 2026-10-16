package com.liceo.campustrack.service;

import com.liceo.campustrack.model.ClassSession;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrCodeServiceTest {

    private final QrCodeService qr = new QrCodeService();

    private static ClassSession session(long id, String secret) {
        ClassSession session = new ClassSession();
        session.setId(id);
        session.setSecret(secret);
        return session;
    }

    @Test
    void codeIsOnlyValidForItsOwnWindowAndSession() {
        ClassSession session = session(7, "secret-one");
        QrCodeService.Token token = qr.parse(qr.sessionPayload(session, 1000));

        assertEquals(7, token.sessionId());
        assertTrue(qr.matches(session, token, 1000));
        assertFalse(qr.matches(session, token, 1001), "an old screenshot must not pass in the next window");
        assertFalse(qr.matches(session(7, "other-secret"), token, 1000), "a code cannot be forged without the secret");
        assertNotEquals(qr.sessionPayload(session, 1000), qr.sessionPayload(session, 1001), "the QR must rotate");
    }

    @Test
    void manualCodeMatchesOnlyItsWindow() {
        ClassSession session = session(12, "secret-two");
        QrCodeService.Token typed = qr.parse(qr.manualCode(session, 500));

        assertNull(typed.window());
        assertTrue(qr.matches(session, typed, 500));
        assertFalse(qr.matches(session, qr.parse("12-000000"), 500) && qr.matches(session, qr.parse("12-000001"), 500));
    }

    @Test
    void rejectsTextThatIsNotASessionCode() {
        assertNull(qr.parse("hello"));
        assertNull(qr.parse("LICEO-ID:2023-00123"));
        assertNull(qr.parse(null));
    }

    @Test
    void qrImageRoundTrip() throws Exception {
        String payload = qr.sessionPayload(session(3, "secret-three"), 42);
        byte[] png = Base64.getDecoder().decode(qr.toPngBase64(payload, 300));
        assertEquals(payload, qr.decodeImage(new ByteArrayInputStream(png)));
        assertNull(qr.decodeImage(new ByteArrayInputStream("not an image".getBytes())));
    }
}
