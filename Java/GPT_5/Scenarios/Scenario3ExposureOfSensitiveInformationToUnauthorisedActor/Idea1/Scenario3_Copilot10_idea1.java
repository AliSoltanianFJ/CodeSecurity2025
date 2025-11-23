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

public class Scenario3_Copilot10_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final int PBKDF2_ITERATIONS = 100_000;
        private static final int KEY_LENGTH_BITS = 256;

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            // prevent caching of authenticated content
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
                return;
            }

            String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "root");

            final String sql = "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username or password was incorrect
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("salt");

                    if (storedHash == null || storedSalt == null) {
                        // Missing hashing data for the account; treat as invalid credentials and log for investigation.
                        LOGGER.log(Level.WARNING, "Account record missing hash/salt for user: {0}", sanitizeForLog(username));
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    boolean verified;
                    try {
                        verified = verifyPassword(password, storedSalt, storedHash);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        LOGGER.log(Level.SEVERE, "Crypto configuration error during login for user: " + sanitizeForLog(username), e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                        return;
                    }

                    if (verified) {
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        // set reasonable session timeout (e.g. 30 minutes)
                        session.setMaxInactiveInterval(30 * 60);

                        resp.setStatus(HttpServletResponse.SC_OK);
                        try (PrintWriter out = resp.getWriter()) {
                            out.println("Login successful");
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                    }
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login attempt for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login for user: " + sanitizeForLog(username), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        /**
         * Verify password using PBKDF2WithHmacSHA256 with Base64-encoded salt and hash stored in DB.
         */
        private static boolean verifyPassword(String password, String base64Salt, String base64ExpectedHash)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            byte[] expected = Base64.getDecoder().decode(base64ExpectedHash);

            byte[] computed = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

            if (computed.length != expected.length) {
                return false;
            }
            // constant-time comparison
            return MessageDigest.isEqual(computed, expected);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
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
