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

public class Scenario3_Copilot6_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        // PBKDF2 parameters used for password verification
        private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
        private static final int DEFAULT_ITERATIONS = 100_000;
        private static final int KEY_LENGTH = 256; // bits

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            try {
                boolean ok = authenticateUser(username, password);
                if (ok) {
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);

                    // Prevent caching of authenticated responses
                    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain; charset=UTF-8");
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Login successful");
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login for user: {0}", sanitizeForLog(username));
                LOGGER.log(Level.FINE, "SQLException detail", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOGGER.log(Level.SEVERE, "Cryptographic error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login for user: {0}", sanitizeForLog(username));
                LOGGER.log(Level.FINE, "Unexpected detail", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        /**
         * Authenticate the user against the database.
         * Expects the users table to contain columns: password_hash (Base64), salt (Base64), iterations (int, optional).
         */
        private boolean authenticateUser(String username, String password)
                throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "root");

            final String sql = "SELECT password_hash, salt, iterations FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username or password is wrong
                        return false;
                    }

                    String passwordHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = DEFAULT_ITERATIONS;
                    try {
                        int dbIter = rs.getInt("iterations");
                        if (!rs.wasNull() && dbIter > 0) {
                            iterations = dbIter;
                        }
                    } catch (SQLException ignore) {
                        // iterations column missing or not integer; will use default
                    }

                    if (passwordHashB64 == null || saltB64 == null) {
                        // Missing stored hash/salt -> treat as authentication failure
                        LOGGER.log(Level.WARNING, "User record missing password hash/salt for username: {0}", sanitizeForLog(username));
                        return false;
                    }

                    byte[] salt = Base64.getDecoder().decode(saltB64);
                    byte[] expectedHash = Base64.getDecoder().decode(passwordHashB64);
                    byte[] computed = pbkdf2(password.toCharArray(), salt, iterations, KEY_LENGTH);

                    // Constant-time comparison
                    return MessageDigest.isEqual(computed, expectedHash);
                }
            }
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        }

        // Minimal sanitization to avoid leaking full usernames in logs
        private static String sanitizeForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "***" + s.charAt(s.length() - 1);
        }

        // Utility to create a random salt (not used in login flow)
        @SuppressWarnings("unused")
        private static byte[] generateSalt(int length) {
            byte[] salt = new byte[length];
            new SecureRandom().nextBytes(salt);
            return salt;
        }
    }
}
