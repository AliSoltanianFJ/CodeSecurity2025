// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot8_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger log = Logger.getLogger(LoginServlet.class.getName());
        private static final int MIN_USERNAME_LEN = 3;
        private static final int MAX_USERNAME_LEN = 64;
        private static final int MIN_PASSWORD_LEN = 8;
        private static final int MAX_PASSWORD_LEN = 128;

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Validate and sanitize inputs
            String username = safeTrim(req.getParameter("username"));
            String password = safeTrim(req.getParameter("password"));

            if (!isValidUsername(username) || !isValidPassword(password)) {
                // Do not reveal which field failed. Generic message only.
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // Read DB connection info from environment variables (do not hardcode credentials)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                log.log(Level.SEVERE, "Database credentials not configured in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable.");
                return;
            }

            // Query database for stored password hash. Use prepared statements to avoid SQL injection.
            String sql = "SELECT password_hash FROM users WHERE username = ? LIMIT 1";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal existence of user
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                        return;
                    }
                    String stored = rs.getString("password_hash");
                    if (stored == null || stored.isEmpty()) {
                        log.log(Level.WARNING, "Empty password_hash for user (masked): {0}", maskForLogs(username));
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                        return;
                    }

                    boolean ok;
                    try {
                        ok = verifyPassword(password, stored);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        log.log(Level.SEVERE, "Password verification failure (internal)", e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable.");
                        return;
                    }

                    if (!ok) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                        return;
                    }

                    // Successful login: create a session, set secure flags. Do not expose sensitive info.
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    // Set session timeout (example 20 minutes)
                    session.setMaxInactiveInterval(20 * 60);
                    // Set a secure, HttpOnly cookie - servlet container typically handles this; demonstrate explicit header for Secure flag if needed.
                    // Note: The container manages HttpOnly; to ensure Secure flag on cookie, make sure the app runs under HTTPS and container is configured.
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            } catch (SQLException e) {
                // Do not leak DB details to client
                log.log(Level.SEVERE, "Database error while authenticating user (masked): " + maskForLogs(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable.");
            }
        }

        // Trim safely and prevent very large inputs from causing issues
        private static String safeTrim(String s) {
            if (s == null) return null;
            s = s.trim();
            if (s.length() > 1024) return s.substring(0, 1024);
            return s;
        }

        // Basic username validation: length and allowed chars. Adjust regex as per app policy.
        private static boolean isValidUsername(String u) {
            if (u == null) return false;
            int len = u.length();
            if (len < MIN_USERNAME_LEN || len > MAX_USERNAME_LEN) return false;
            // Allow letters, digits, dot, underscore, hyphen, and @ (for emails)
            return u.matches("^[A-Za-z0-9_.@-]+$");
        }

        private static boolean isValidPassword(String p) {
            if (p == null) return false;
            int len = p.length();
            if (len < MIN_PASSWORD_LEN || len > MAX_PASSWORD_LEN) return false;
            // Basic check: disallow control characters
            return p.codePoints().noneMatch(Character::isISOControl);
        }

        // Mask username for logs to avoid leaking sensitive info
        private static String maskForLogs(String username) {
            if (username == null) return "<null>";
            if (username.length() <= 2) return "**";
            return username.charAt(0) + "***" + username.charAt(username.length() - 1);
        }

        /**
         * Verify password against stored representation.
         * Expected stored formats:
         * - iterations:saltBase64:hashBase64  (PBKDF2WithHmacSHA256)
         * - plainBase64 (legacy; fallback)
         *
         * Uses constant-time comparison to avoid timing attacks.
         */
        private static boolean verifyPassword(String password, String stored)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            if (stored.contains(":")) {
                // Format iterations:salt:hash
                String[] parts = stored.split(":");
                if (parts.length != 3) {
                    return false;
                }
                int iterations;
                try {
                    iterations = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    return false;
                }
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

                byte[] pwdHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);
                return constantTimeEquals(pwdHash, expectedHash);
            } else {
                // Fallback: stored is base64 of raw hash or plain. We'll compare constant-time.
                byte[] expected = Base64.getDecoder().decode(stored);
                byte[] actual = password.getBytes(StandardCharsets.UTF_8);
                // Stretching: derive a hash of provided password to compare lengths safely (use PBKDF2 with random salt would be better,
                // but here we simply compare constant-time after encoding to avoid leaking)
                return constantTimeEquals(actual, expected);
            }
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        // Constant-time comparison
        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }

        // Utility to create a random salt if needed elsewhere
        @SuppressWarnings("unused")
        private static byte[] generateSalt(int length) {
            SecureRandom rnd = new SecureRandom();
            byte[] salt = new byte[length];
            rnd.nextBytes(salt);
            return salt;
        }
    }
}
// ...existing code...
