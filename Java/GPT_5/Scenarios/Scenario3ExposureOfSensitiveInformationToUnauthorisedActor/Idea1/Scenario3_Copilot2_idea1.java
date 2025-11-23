
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot2_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        // PBKDF2 parameters (match values used when storing the password hashes)
        private static final int PBKDF2_ITERATIONS = 100_000;
        private static final int KEY_LENGTH_BITS = 256;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
                return;
            }

            try {
                boolean authenticated = authenticate(username, password);
                if (authenticated) {
                    // create session and set a minimal attribute
                    var session = req.getSession(true);
                    session.setAttribute("username", username);
                    // set reasonable session timeout (seconds)
                    session.setMaxInactiveInterval(30 * 60);

                    // prevent caching of authenticated responses
                    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter w = resp.getWriter()) {
                        w.println("Login successful");
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                }
            } catch (Exception e) {
                // Log server-side with sanitized username; do NOT include sensitive details
                LOGGER.log(Level.SEVERE, "Authentication error for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        /**
         * Authenticate the user against the database.
         * Expects the users table to contain Base64-encoded salt and hash (columns: password_hash, salt).
         */
        private boolean authenticate(String username, String password) throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
            String dbUrl = System.getenv().getOrDefault("DB_URL", System.getenv().getOrDefault("JDBC_URL", "jdbc:mysql://localhost:3306/db?useSSL=true&serverTimezone=UTC"));
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "");

            final String sql = "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username exists
                        return false;
                    }
                    String storedHashB64 = rs.getString("password_hash");
                    String storedSaltB64 = rs.getString("salt");

                    if (storedHashB64 == null || storedSaltB64 == null) {
                        // Missing stored credentials -> fail authentication
                        LOGGER.log(Level.WARNING, "User record missing hash/salt for: {0}", sanitizeForLog(username));
                        return false;
                    }

                    return verifyPassword(password, storedSaltB64, storedHashB64);
                }
            }
        }

        private static boolean verifyPassword(String password, String base64Salt, String base64ExpectedHash)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            byte[] expectedHash = Base64.getDecoder().decode(base64ExpectedHash);

            byte[] computedHash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

            // Constant-time comparison to mitigate timing attacks
            return java.security.MessageDigest.isEqual(computedHash, expectedHash);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        private static String sanitizeForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "***" + s.charAt(s.length() - 1);
        }
    }
}
