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
import java.security.spec.KeySpec;
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

public class Scenario3_Copilot5_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */

        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final int DEFAULT_ITERATIONS = 100_000;
        private static final int DEFAULT_KEY_LENGTH = 256; // bits

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            // Read DB config from environment when available; use safe defaults for local testing
            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "root");

            String sql = "SELECT password_hash, salt, iterations FROM users WHERE username = ? LIMIT 1";

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
                    String storedSaltB64 = rs.getString("salt");
                    int iterations = DEFAULT_ITERATIONS;
                    try {
                        int it = rs.getInt("iterations");
                        if (!rs.wasNull() && it > 0) iterations = it;
                    } catch (SQLException ignored) {
                        // column may not exist or be null; use default
                    }

                    if (storedHashB64 == null || storedSaltB64 == null) {
                        // Missing stored credentials; treat as auth failure
                        LOGGER.log(Level.WARNING, "User record missing credentials for username: {0}", sanitizeForLog(username));
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    boolean verified;
                    try {
                        verified = verifyPassword(password, storedSaltB64, storedHashB64, iterations, DEFAULT_KEY_LENGTH);
                    } catch (NoSuchAlgorithmException | RuntimeException e) {
                        // Crypto algorithm missing or other unexpected crypto error
                        LOGGER.log(Level.SEVERE, "Crypto error during login for user: " + sanitizeForLog(username), e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                        return;
                    }

                    if (!verified) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    // Successful login: create session
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    // set reasonable session timeout (e.g. 30 minutes)
                    session.setMaxInactiveInterval(30 * 60);

                    // prevent caching of authenticated content
                    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain; charset=UTF-8");
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Login successful");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        private static boolean verifyPassword(String password, String base64Salt, String base64ExpectedHash,
                                              int iterations, int keyLengthBits) throws NoSuchAlgorithmException {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            byte[] expected = Base64.getDecoder().decode(base64ExpectedHash);

            byte[] computed = pbkdf2(password.toCharArray(), salt, iterations, keyLengthBits);
            if (computed == null || computed.length != expected.length) {
                return false;
            }
            // Constant-time comparison
            return MessageDigest.isEqual(computed, expected);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException {
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
                return skf.generateSecret(spec).getEncoded();
            } catch (NoSuchAlgorithmException e) {
                throw e;
            } catch (Exception e) {
                // Wrap other exceptions as runtime to be handled by caller
                throw new RuntimeException("PBKDF2 error", e);
            }
        }

        private static String sanitizeForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "***" + s.charAt(s.length() - 1);
        }
    }
}
// ...existing code...
