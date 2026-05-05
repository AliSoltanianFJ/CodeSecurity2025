// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot1_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        // Default PBKDF2 parameters if not stored per-user
        private static final int DEFAULT_PBKDF2_ITERATIONS = 100_000;
        private static final int DEFAULT_KEY_LENGTH_BITS = 256;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
                return;
            }

            // Read DB config from environment where available (avoid hard-coding in production)
            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "root");

            // The query expects stored base64-encoded salt and base64-encoded PBKDF2 hash.
            // Optionally an iterations column can be present; if not, defaults are used.
            final String sql = "SELECT password_hash, salt, iterations FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username or password was incorrect
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    String storedHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = DEFAULT_PBKDF2_ITERATIONS;
                    try {
                        int dbIterations = rs.getInt("iterations");
                        if (!rs.wasNull() && dbIterations > 0) iterations = dbIterations;
                    } catch (SQLException e) {
                        // Ignore and use default iterations
                    }

                    if (storedHashB64 == null || saltB64 == null) {
                        // Missing required authentication material for this account: fail safely
                        LOGGER.log(Level.WARNING, "User record missing hash/salt for username: {0}", sanitizeForLog(username));
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    boolean verified;
                    try {
                        verified = verifyPasswordPBKDF2(password, saltB64, storedHashB64, iterations, DEFAULT_KEY_LENGTH_BITS);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException ex) {
                        // Crypto failure or bad data; log server-side, do not reveal details to client
                        LOGGER.log(Level.SEVERE, "Password verification error for user: " + sanitizeForLog(username), ex);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                        return;
                    }

                    if (!verified) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    // Successful login: create session, set minimal attributes, prevent caching
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    session.setMaxInactiveInterval(30 * 60); // 30 minutes

                    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Login successful");
                    }
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login attempt for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        /**
         * Verify a password against a stored PBKDF2 (HMAC-SHA256) hash encoded in Base64.
         *
         * @param password         Plaintext password provided by the user
         * @param saltB64          Base64-encoded salt
         * @param expectedHashB64  Base64-encoded derived key/hash
         * @param iterations       PBKDF2 iterations
         * @param keyLengthBits    Derived key length in bits
         * @return true if password matches the stored hash (constant-time comparison), false otherwise
         */
        private static boolean verifyPasswordPBKDF2(String password, String saltB64, String expectedHashB64,
                                                    int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {

            byte[] salt = Base64.getDecoder().decode(saltB64);
            byte[] expectedHash = Base64.getDecoder().decode(expectedHashB64);

            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computed = skf.generateSecret(spec).getEncoded();

            // Use MessageDigest.isEqual for constant-time comparison
            return MessageDigest.isEqual(computed, expectedHash);
        }

        private static String sanitizeForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "***" + s.charAt(s.length() - 1);
        }
    }
}
// ...existing code...
