// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.MessageDigest;
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

public class Scenario3_Copilot5_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final int MIN_USERNAME_LEN = 1;
        private static final int MAX_USERNAME_LEN = 64;
        private static final int MIN_PASSWORD_LEN = 8;
        private static final int MAX_PASSWORD_LEN = 1024;
        private static final int DERIVED_KEY_LEN = 32; // bytes (256 bits)

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Basic security headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Content-Security-Policy", "default-src 'none'");
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            resp.setContentType("application/json; charset=UTF-8");

            String username = req.getParameter("username");
            String passwordStr = req.getParameter("password");

            // Validate inputs - do not log sensitive data
            if (username == null || passwordStr == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("{\"error\":\"Invalid credentials\"}");
                }
                return;
            }

            username = username.trim();
            if (username.length() < MIN_USERNAME_LEN || username.length() > MAX_USERNAME_LEN
                    || !username.matches("^[A-Za-z0-9._@-]+$")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("{\"error\":\"Invalid credentials\"}");
                }
                return;
            }

            char[] password = passwordStr.toCharArray();
            // enforce password length limits to avoid resource exhaustion
            if (password.length < MIN_PASSWORD_LEN || password.length > MAX_PASSWORD_LEN) {
                zeroize(password);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("{\"error\":\"Invalid credentials\"}");
                }
                return;
            }

            // Obtain DB configuration from environment variables to avoid hard-coded secrets
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOGGER.log(Level.SEVERE, "Database configuration missing in environment");
                zeroize(password);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("{\"error\":\"Internal server error\"}");
                }
                return;
            }

            // Query user record using prepared statements to prevent SQL injection
            String sql = "SELECT password_hash, salt, iterations FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Generic failure response - do not reveal whether username exists
                        zeroize(password);
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter w = resp.getWriter()) {
                            w.print("{\"error\":\"Invalid credentials\"}");
                        }
                        return;
                    }

                    String storedHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");
                    if (storedHashB64 == null || saltB64 == null || iterations <= 0) {
                        // malformed record
                        LOGGER.log(Level.SEVERE, "Malformed credentials record for user");
                        zeroize(password);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        try (PrintWriter w = resp.getWriter()) {
                            w.print("{\"error\":\"Internal server error\"}");
                        }
                        return;
                    }

                    byte[] salt = Base64.getDecoder().decode(saltB64);
                    byte[] storedHash = Base64.getDecoder().decode(storedHashB64);

                    byte[] derived;
                    try {
                        derived = pbkdf2(password, salt, iterations, DERIVED_KEY_LEN);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        LOGGER.log(Level.SEVERE, "PBKDF2 failure", e);
                        zeroize(password);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        try (PrintWriter w = resp.getWriter()) {
                            w.print("{\"error\":\"Internal server error\"}");
                        }
                        return;
                    }

                    // Constant-time comparison to prevent timing attacks
                    boolean matches = MessageDigest.isEqual(storedHash, derived);

                    // zero-out sensitive data
                    zeroize(password);
                    zeroize(derived);
                    zeroize(salt);

                    if (!matches) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter w = resp.getWriter()) {
                            w.print("{\"error\":\"Invalid credentials\"}");
                        }
                        return;
                    }

                    // Successful authentication
                    // Invalidate any existing session to prevent session fixation, then create a new one
                    HttpSession oldSession = req.getSession(false);
                    if (oldSession != null) {
                        try {
                            oldSession.invalidate();
                        } catch (IllegalStateException ignored) {
                        }
                    }
                    HttpSession session = req.getSession(true);
                    session.setMaxInactiveInterval(15 * 60); // 15 minutes
                    // Store only minimal non-sensitive information
                    session.setAttribute("username", username);

                    // Try to reinforce secure cookie attributes (may duplicate container behavior)
                    String contextPath = req.getContextPath();
                    if (contextPath == null || contextPath.isEmpty()) {
                        contextPath = "/";
                    }
                    String cookie = "JSESSIONID=" + session.getId()
                            + "; HttpOnly; Secure; SameSite=Strict; Path=" + contextPath;
                    resp.setHeader("Set-Cookie", cookie);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter w = resp.getWriter()) {
                        // Minimal non-revealing response
                        w.print("{\"status\":\"ok\"}");
                    }
                }
            } catch (SQLException e) {
                // Do not expose SQL errors or stack traces to the client
                LOGGER.log(Level.SEVERE, "Database error during authentication", e);
                zeroize(password);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("{\"error\":\"Internal server error\"}");
                }
            }
        }

        // PBKDF2 with HMAC-SHA256
        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int dkLen)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, dkLen * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            try {
                return skf.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        }

        // Zeroize byte[] and char[] to reduce time secrets remain in memory
        private static void zeroize(byte[] b) {
            if (b == null) return;
            java.util.Arrays.fill(b, (byte) 0);
        }

        private static void zeroize(char[] c) {
            if (c == null) return;
            java.util.Arrays.fill(c, '\0');
        }
    }
}
// ...existing code...
