// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
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
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot2_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        // Input validation constraints
        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
        private static final int PASSWORD_MIN = 8;
        private static final int PASSWORD_MAX = 128;

        // PBKDF2 parameters - used to verify against stored values
        private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Ensure we don't cache authentication responses
            resp.setHeader("Cache-Control", "no-store");
            resp.setHeader("Pragma", "no-cache");
            resp.setContentType("application/json; charset=UTF-8");

            String username = sanitize(req.getParameter("username"));
            String password = req.getParameter("password"); // keep raw for KDF check; will validate length

            // Basic input validation
            if (username == null || password == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, "{\"status\":\"error\",\"message\":\"Invalid credentials.\"}");
                return;
            }
            if (!validateUsername(username) || !validatePassword(password)) {
                // Do not reveal which field failed
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, "{\"status\":\"error\",\"message\":\"Invalid credentials.\"}");
                return;
            }

            // Get DB connection info from environment to avoid hardcoding secrets
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (isNullOrEmpty(dbUrl) || isNullOrEmpty(dbUser) || dbPass == null) {
                LOGGER.log(Level.SEVERE, "Database credentials not configured in environment");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(resp, "{\"status\":\"error\",\"message\":\"Server error.\"}");
                return;
            }

            // Query safely using prepared statements and verify password using constant-time comparison
            String query = "SELECT password_hash, salt, iterations FROM users WHERE username = ? AND active = 1 LIMIT 1";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Uniform response for not found / wrong password
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        writeJson(resp, "{\"status\":\"error\",\"message\":\"Authentication failed.\"}");
                        return;
                    }

                    String storedHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");

                    if (isNullOrEmpty(storedHashB64) || isNullOrEmpty(saltB64) || iterations <= 0) {
                        LOGGER.log(Level.WARNING, "User record missing hashing metadata for user: {0}", username);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        writeJson(resp, "{\"status\":\"error\",\"message\":\"Server error.\"}");
                        return;
                    }

                    boolean verified = verifyPassword(password, storedHashB64, saltB64, iterations);
                    if (!verified) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        writeJson(resp, "{\"status\":\"error\",\"message\":\"Authentication failed.\"}");
                        return;
                    }

                    // Successful authentication:
                    // - invalidate any existing session to prevent fixation
                    if (req.getSession(false) != null) {
                        try {
                            req.getSession(false).invalidate();
                        } catch (IllegalStateException ignored) {
                            // ignore invalidation race
                        }
                    }
                    // create a new session
                    var session = req.getSession(true);
                    session.setAttribute("username", username);
                    // session timeout in seconds (example: 15 minutes)
                    session.setMaxInactiveInterval(15 * 60);

                    // Try to set secure cookie flags for session id
                    try {
                        String sessionId = session.getId();
                        Cookie sidCookie = new Cookie("JSESSIONID", sessionId);
                        sidCookie.setHttpOnly(true);
                        sidCookie.setSecure(req.isSecure()); // true when request uses HTTPS
                        sidCookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
                        // Do not set a long expiration (session cookie)
                        sidCookie.setMaxAge(-1);
                        resp.addCookie(sidCookie);
                    } catch (Exception ex) {
                        // If we cannot set the cookie, still continue container may manage the cookie
                        LOGGER.log(Level.FINE, "Could not set session cookie flags explicitly: {0}", ex.getMessage());
                    }

                    resp.setStatus(HttpServletResponse.SC_OK);
                    writeJson(resp, "{\"status\":\"ok\"}");
                }
            } catch (SQLException e) {
                // Do not leak DB details to client
                LOGGER.log(Level.SEVERE, "Database error during authentication: {0}", e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(resp, "{\"status\":\"error\",\"message\":\"Server error.\"}");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during authentication: {0}", e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                writeJson(resp, "{\"status\":\"error\",\"message\":\"Server error.\"}");
            }
        }

        private static boolean validateUsername(String username) {
            return username != null && USERNAME_PATTERN.matcher(username).matches();
        }

        private static boolean validatePassword(String password) {
            if (password == null) return false;
            int len = password.length();
            return len >= PASSWORD_MIN && len <= PASSWORD_MAX;
        }

        private static boolean isNullOrEmpty(String s) {
            return s == null || s.isEmpty();
        }

        private static String sanitize(String input) {
            if (input == null) return null;
            // Basic sanitization: trim and remove control characters
            String trimmed = input.trim();
            // Remove any control (non-printable) characters
            StringBuilder sb = new StringBuilder(trimmed.length());
            for (char c : trimmed.toCharArray()) {
                if (c >= 0x20 && c != 0x7F) {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static void writeJson(HttpServletResponse resp, String json) throws IOException {
            // Minimal JSON writer; content type already set
            try (PrintWriter out = resp.getWriter()) {
                out.write(json);
                out.flush();
            } catch (IllegalStateException e) {
                // response.getWriter() may throw if output stream used; avoid stacktrace to client
                LOGGER.log(Level.WARNING, "Could not write response body: {0}", e.getMessage());
            }
        }

        /**
         * Verify a password using PBKDF2(HMAC-SHA256). Stored values are expected to be Base64.
         * Uses constant-time comparison to avoid timing attacks.
         */
        private static boolean verifyPassword(String password, String storedHashB64, String saltB64, int iterations) {
            try {
                byte[] salt = Base64.getDecoder().decode(saltB64);
                byte[] storedHash = Base64.getDecoder().decode(storedHashB64);

                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, storedHash.length * 8);
                SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGORITHM);
                byte[] computed = skf.generateSecret(spec).getEncoded();

                // Constant-time comparison
                return MessageDigest.isEqual(storedHash, computed);
            } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                LOGGER.log(Level.SEVERE, "Password verification error: {0}", ex.getMessage());
                return false;
            }
        }
    }
}
// ...existing code...
