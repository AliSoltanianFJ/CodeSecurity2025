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

public class Scenario3_Copilot7_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        // PBKDF2 parameters - tune to your environment
        private static final int PBKDF2_ITERATIONS = 100_000;
        private static final int KEY_LENGTH = 256; // bits

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password required");
                return;
            }

            boolean authenticated;
            try {
                authenticated = login(username, password);
            } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                // Log server-side with sanitized username; do not leak internal details to client
                LOGGER.log(Level.SEVERE, "Authentication error for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            if (authenticated) {
                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);
                session.setMaxInactiveInterval(30 * 60); // 30 minutes

                // Prevent caching of authenticated responses
                resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);

                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Login successful");
                }
            } else {
                // Generic error to avoid revealing whether username or password was incorrect
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            }
        }

        /**
         * Authenticate user against the database.
         * Expects users table with base64-encoded salt and base64-encoded pbkdf2 hash in columns
         * 'salt' and 'password_hash' respectively.
         */
        private boolean login(String username, String password)
                throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {

            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "root");

            final String sql = "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }

                    String storedHashB64 = rs.getString("password_hash");
                    String storedSaltB64 = rs.getString("salt");
                    if (storedHashB64 == null || storedSaltB64 == null) {
                        LOGGER.log(Level.WARNING, "Missing password hash/salt for user: {0}", sanitizeForLog(username));
                        return false;
                    }

                    byte[] salt = Base64.getDecoder().decode(storedSaltB64);
                    byte[] expectedHash = Base64.getDecoder().decode(storedHashB64);

                    byte[] computed = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);

                    return MessageDigest.isEqual(computed, expectedHash);
                }
            }
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
// ...existing code...
