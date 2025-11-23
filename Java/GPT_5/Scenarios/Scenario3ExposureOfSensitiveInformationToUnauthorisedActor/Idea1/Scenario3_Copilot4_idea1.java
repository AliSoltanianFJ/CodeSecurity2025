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

public class Scenario3_Copilot4_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            // Read DB config from environment when possible; use safe defaults for local testing only.
            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db?useSSL=true&serverTimezone=UTC");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "");

            // Query should return the stored password hash and salt for the user.
            final String sql = "SELECT password_hash, salt, iterations, key_length FROM users WHERE username = ? LIMIT 1";

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
                    int iterations = 0;
                    int keyLength = 0;
                    try {
                        iterations = rs.getInt("iterations");
                        keyLength = rs.getInt("key_length");
                    } catch (Exception ignored) {
                        // If DB doesn't provide iterations/key_length, we'll use safe defaults below.
                    }

                    if (storedHashB64 == null || saltB64 == null) {
                        LOGGER.log(Level.WARNING, "User record missing password or salt for username: {0}", maskForLog(username));
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    // Defaults if DB omitted values
                    if (iterations <= 0) iterations = 100_000; // reasonable default iterations
                    if (keyLength <= 0) keyLength = 256;

                    boolean verified;
                    try {
                        verified = verifyPassword(password.toCharArray(), saltB64, storedHashB64, iterations, keyLength);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        LOGGER.log(Level.SEVERE, "Crypto error during login for user: " + maskForLog(username), e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                        return;
                    }

                    if (!verified) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    // Successful login: create session and set attribute
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    session.setMaxInactiveInterval(30 * 60); // 30 minutes

                    // Prevent caching of authenticated content
                    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Login successful");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login for user: " + maskForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        /**
         * Verify password using PBKDF2WithHmacSHA256. Stored values are expected to be Base64-encoded.
         */
        private static boolean verifyPassword(char[] password, String base64Salt, String base64ExpectedHash,
                                              int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {

            byte[] salt = Base64.getDecoder().decode(base64Salt);
            byte[] expectedHash = Base64.getDecoder().decode(base64ExpectedHash);

            byte[] computed = pbkdf2(password, salt, iterations, keyLengthBits);

            // constant-time comparison
            return MessageDigest.isEqual(computed, expectedHash);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        // Minimal masking for logs to avoid leaking full username
        private static String maskForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "***" + s.charAt(s.length() - 1);
        }

        // Utility to create salt + hash (not used in login but provided for completeness)
        @SuppressWarnings("unused")
        private static String[] createSaltedHash(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            int iterations = 100_000;
            int keyLength = 256;
            byte[] hash = pbkdf2(password, salt, iterations, keyLength);
            return new String[]{Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash)};
        }
    }
}
// ...existing code...
