// ...existing code...
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
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
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot3_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        // PBKDF2 parameters - adjust as needed for your deployment policy
        private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
        private static final int PBKDF2_ITERATIONS = 100_000;
        private static final int KEY_LENGTH_BITS = 256;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password required");
                return;
            }

            try {
                boolean ok = authenticate(username, password);
                if (ok) {
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    // set reasonable session timeout (seconds)
                    session.setMaxInactiveInterval(30 * 60);

                    // Prevent caching of authenticated responses
                    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter w = resp.getWriter()) {
                        w.println("Login successful");
                    }
                } else {
                    // Do not reveal whether username or password was incorrect
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOGGER.log(Level.SEVERE, "Crypto error during login for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        /**
         * Authenticate the user against the database.
         * Expects users table with columns: username, password_hash (Base64), salt (Base64).
         */
        private boolean authenticate(String username, String password)
                throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUrl.isEmpty()) {
                dbUrl = "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC";
            }
            if (dbUser == null) dbUser = "root";
            if (dbPass == null) dbPass = "root";

            final String sql = "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // user not found
                        return false;
                    }

                    String storedHashB64 = rs.getString("password_hash");
                    String storedSaltB64 = rs.getString("salt");

                    if (storedHashB64 == null || storedSaltB64 == null) {
                        // Missing fields -> treat as authentication failure
                        LOGGER.log(Level.WARNING, "User record missing credentials for username: {0}", sanitizeForLog(username));
                        return false;
                    }

                    return verifyPassword(password, storedSaltB64, storedHashB64);
                }
            }
        }

        /**
         * Verify a password using PBKDF2 with the provided Base64-encoded salt and expected hash.
         * Uses constant-time comparison to avoid timing attacks.
         */
        private boolean verifyPassword(String password, String base64Salt, String base64ExpectedHash)
                throws NoSuchAlgorithmException, InvalidKeySpecException {

            byte[] salt = Base64.getDecoder().decode(base64Salt);
            byte[] expectedHash = Base64.getDecoder().decode(base64ExpectedHash);

            byte[] computed = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

            // Constant-time comparison
            return MessageDigest.isEqual(computed, expectedHash);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        }

        private static String sanitizeForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "***" + s.charAt(s.length() - 1);
        }

        // Optional helper to create a new salt (not used in login flow)
        @SuppressWarnings("unused")
        private static String createSaltBase64() {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        }
    }
}
// ...existing code...
