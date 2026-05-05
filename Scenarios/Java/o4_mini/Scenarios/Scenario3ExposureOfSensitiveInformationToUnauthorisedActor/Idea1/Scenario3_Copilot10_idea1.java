// ...existing code...
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
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot10_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = safeTrim(req.getParameter("username"));
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                // Generic message; do not reveal which field is missing
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            // Read DB connection settings from environment variables to avoid hardcoding credentials.
            String url = System.getenv("DB_URL");        // e.g. jdbc:mysql://localhost:3306/db
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (url == null || dbUser == null || dbPass == null) {
                getServletContext().log("Database connection settings are not configured in environment variables.");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error.");
                return;
            }

            String sql = "SELECT password_hash FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether user exists
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    if (storedHash == null || storedHash.isEmpty()) {
                        getServletContext().log("Stored password is empty for user: " + username);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error.");
                        return;
                    }

                    boolean ok;
                    try {
                        ok = verifyPBKDF2Password(password, storedHash);
                    } catch (IllegalArgumentException e) {
                        // Stored hash has an unexpected format
                        getServletContext().log("Invalid password hash format for user: " + username, e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error.");
                        return;
                    }

                    if (ok) {
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        // Do not expose sensitive information to the client. Redirect to post-login page.
                        resp.sendRedirect(req.getContextPath() + "/home.jsp");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                    }
                }

            } catch (SQLException e) {
                // Log details to server logs only
                getServletContext().log("Database error during login attempt for user: " + username, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            }
        }

        // Helper: trims null-safe
        private static String safeTrim(String s) {
            return s == null ? null : s.trim();
        }

        /*
         Expected storedHash format (single string): iterations:saltBase64:hashBase64
         - iterations: decimal integer (e.g. 65536)
         - saltBase64: Base64-encoded salt bytes
         - hashBase64: Base64-encoded derived key bytes (PBKDF2 output)
         This format is common and avoids storing raw bytes in DB.
         */
        private static boolean verifyPBKDF2Password(String password, String storedHash) {
            String[] parts = storedHash.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid stored password hash format.");
            }

            int iterations;
            try {
                iterations = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid iteration count.", e);
            }

            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            byte[] derived;
            try {
                derived = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                // Re-throw as runtime to be handled by caller
                throw new IllegalArgumentException("Password derivation error.", ex);
            }

            return constantTimeEquals(expectedHash, derived);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            if (a.length != b.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }

        // (Optional) utility to create a new storedHash using secure defaults.
        // Not used by login flow but provided for completeness.
        @SuppressWarnings("unused")
        private static String createPBKDF2Hash(String password) {
            int iterations = 100_000;
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash;
            try {
                hash = pbkdf2(password.toCharArray(), salt, iterations, 32 * 8); // 32 bytes = 256 bits
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException("Failed to generate password hash", e);
            }
            return iterations + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        }
    }
}
// ...existing code...
