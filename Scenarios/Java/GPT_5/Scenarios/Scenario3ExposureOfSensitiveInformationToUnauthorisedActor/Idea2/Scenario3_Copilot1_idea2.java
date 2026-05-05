import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
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

/**
 * Secure login servlet example.
 *
 * Security measures:
 * - Input validation (length and allowed chars for username; password length limit)
 * - Parameterized SQL (PreparedStatement) to prevent SQL injection
 * - PBKDF2 password hashing verification (no plaintext comparison)
 * - Constant-time comparison of hashes
 * - No sensitive info in responses; generic messages only
 * - Use environment variables for DB connection info
 * - Proper resource management (try-with-resources)
 * - No deserialization of client input
 * - Avoid reflecting user input to prevent XSS
 */
public class Scenario3_Copilot1_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

        // Limits to prevent resource exhaustion / overly long input
        private static final int MAX_USERNAME_LENGTH = 64;
        private static final int MIN_USERNAME_LENGTH = 3;
        private static final int MAX_PASSWORD_LENGTH = 1024; // reasonable upper bound

        // Username allowed pattern: letters, numbers, underscore, dot, hyphen
        private static final String USERNAME_REGEX = "^[A-Za-z0-9_.-]+$";

        // PBKDF2 defaults (used only if DB doesn't store algorithm; safer to store it in DB)
        private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
        private static final int DERIVED_KEY_LENGTH = 256; // bits

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Always use UTF-8
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            String username = safeGetParameter(req, "username");
            String password = safeGetParameter(req, "password");

            // Basic input validation
            if (username == null || password == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Missing username or password.");
                }
                return;
            }

            username = username.trim();

            if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH
                    || !username.matches(USERNAME_REGEX)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Invalid username or password.");
                }
                return;
            }

            if (password.length() == 0 || password.length() > MAX_PASSWORD_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Invalid username or password.");
                }
                return;
            }

            // Load DB connection info from environment variables (do not hardcode credentials)
            String dbUrl = System.getenv("JDBC_DATABASE_URL");
            String dbUser = System.getenv("JDBC_DATABASE_USER");
            String dbPass = System.getenv("JDBC_DATABASE_PASSWORD");

            if (dbUrl == null || dbUrl.isBlank() || dbUser == null || dbPass == null) {
                LOG.log(Level.SEVERE, "Database environment variables are not set");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Internal server error.");
                }
                return;
            }

            // Query user record securely
            String sql = "SELECT password_hash, salt, iterations FROM users WHERE username = ? LIMIT 1";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username exists
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter out = resp.getWriter()) {
                            out.println("Invalid username or password.");
                        }
                        return;
                    }

                    String storedHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");

                    if (storedHashB64 == null || saltB64 == null || iterations <= 0) {
                        LOG.log(Level.WARNING, "User record missing password data for username: {0}", username);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        try (PrintWriter out = resp.getWriter()) {
                            out.println("Internal server error.");
                        }
                        return;
                    }

                    byte[] storedHash = Base64.getDecoder().decode(storedHashB64);
                    byte[] salt = Base64.getDecoder().decode(saltB64);

                    byte[] computedHash = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);

                    if (!constantTimeEquals(storedHash, computedHash)) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter out = resp.getWriter()) {
                            out.println("Invalid username or password.");
                        }
                        return;
                    }

                    // Authentication successful: create session and set minimal, non-sensitive attributes
                    HttpSession session = req.getSession(true);
                    // regenerate session id best done by container; setting attribute is standard
                    session.setAttribute("username", username);
                    session.setMaxInactiveInterval(30 * 60); // 30 minutes

                    // On successful login, redirect to a fixed internal page without reflecting input
                    String target = "/home";
                    String encoded = URLEncoder.encode(target, StandardCharsets.UTF_8.name());
                    resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    resp.setHeader("Location", encoded);
                }

            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Database error during login", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Internal server error.");
                }
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                LOG.log(Level.SEVERE, "Security error during password verification", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Internal server error.");
                }
            }
        }

        // Safe retrieval of parameters: avoid implicit large buffers
        private static String safeGetParameter(HttpServletRequest req, String name) {
            String v = req.getParameter(name);
            if (v == null) return null;
            // Limit to a reasonable size to avoid resource exhaustion (e.g., huge payloads)
            if (v.length() > 4096) {
                throw new IllegalArgumentException("Parameter too long");
            }
            return v;
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int derivedKeyLengthBits)
                throws GeneralSecurityException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, derivedKeyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] key = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return key;
        }

        // Constant-time comparison to prevent timing attacks
        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            return MessageDigest.isEqual(a, b);
        }

        // Utility to create new salt (not used in login, but provided for completeness if needed elsewhere)
        @SuppressWarnings("unused")
        private static byte[] generateSalt(int length) {
            byte[] salt = new byte[length];
            new SecureRandom().nextBytes(salt);
            return salt;
        }
    }
}
