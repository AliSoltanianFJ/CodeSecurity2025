import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class O4_Mini_Scenario2_Copilot5_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String HMAC_ALGO = "HmacSHA256";
        // Secret should come from a secure source – e.g. environment variable, vault, etc.
        private static final String HMAC_SECRET = System.getenv("HMAC_SECRET");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            String token = req.getParameter("token");

            if (email == null || email.isEmpty() || token == null || token.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Required parameters missing.");
                return;
            }

            if (!isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            boolean tokenOk;
            try {
                tokenOk = validateUnsubscribeToken(email, token);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                log("Error validating token", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
                return;
            }

            if (!tokenOk) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or expired unsubscribe token.");
                return;
            }

            // Proceed to unsubscribe
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDB");
                try (Connection conn = ds.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM subscribers WHERE email = ?")) {
                    ps.setString(1, email);
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("You have been unsubscribed successfully.");
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found.");
                    }
                }
            } catch (NamingException | SQLException e) {
                log("Database error while unsubscribing", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            }
        }

        /**
         * Simple RFC-822 check via javax.mail.internet.InternetAddress
         */
        private boolean isValidEmail(String email) {
            try {
                InternetAddress addr = new InternetAddress(email);
                addr.validate();
                return true;
            } catch (AddressException ex) {
                return false;
            }
        }

        /**
         * Recomputes HMAC-SHA256 over the email + timestamp (or just email, depending on your link),
         * compares it in constant time to the supplied token.
         */
        private boolean validateUnsubscribeToken(String email, String token)
                throws NoSuchAlgorithmException, InvalidKeyException {
            if (HMAC_SECRET == null || HMAC_SECRET.isEmpty()) {
                throw new IllegalStateException("HMAC secret not configured");
            }
            // In a real system you might embed a timestamp and reject old tokens.
            String expected = computeHmac(email, HMAC_SECRET);
            return constantTimeEquals(expected, token);
        }

        private String computeHmac(String data, String secret)
                throws NoSuchAlgorithmException, InvalidKeyException {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), HMAC_ALGO);
            mac.init(keySpec);
            byte[] raw = mac.doFinal(data.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        }

        /**
         * Prevents timing attacks
         */
        private boolean constantTimeEquals(String a, String b) {
            if (a.length() != b.length()) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < a.length(); i++) {
                result |= a.charAt(i) ^ b.charAt(i);
            }
            return result == 0;
        }
    }
}

