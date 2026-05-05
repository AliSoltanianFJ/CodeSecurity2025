
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot9_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final int PBKDF2_ITERATIONS = 100_000;
        private static final int KEY_LENGTH_BITS = 256;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            try {
                boolean ok = authenticate(username, password);
                if (ok) {
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    // set a reasonable session timeout (seconds)
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
                    // Do not reveal which part failed
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login for user: {0}", maskForLog(username));
                LOGGER.log(Level.FINEST, "DB exception detail", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOGGER.log(Level.SEVERE, "Crypto error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        /**
         * Authenticate against the database. Expects table 'users' with columns:
         *   password_hash (Base64-encoded PBKDF2 hash) and salt (Base64-encoded salt).
         *
         * This method uses a prepared statement and constant-time comparison.
         */
        private boolean authenticate(String username, String password)
                throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {

            String dbUrl = System.getenv().getOrDefault("DB_URL",
                    "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASSWORD", "root");

            final String sql = "SELECT password_hash, salt FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // user not found
                        // perform a dummy PBKDF2 to make timing similar to a found user
                        dummyWork();
                        return false;
                    }

                    String b64Hash = rs.getString("password_hash");
                    String b64Salt = rs.getString("salt");

                    if (b64Hash == null || b64Salt == null) {
                        // Missing fields; treat as authentication failure
                        dummyWork();
                        return false;
                    }

                    byte[] salt;
                    byte[] expectedHash;
                    try {
                        salt = Base64.getDecoder().decode(b64Salt);
                        expectedHash = Base64.getDecoder().decode(b64Hash);
                    } catch (IllegalArgumentException e) {
                        // corrupt encoding; treat as failure
                        LOGGER.log(Level.WARNING, "Stored credentials have invalid encoding for user: {0}", maskForLog(username));
                        return false;
                    }

                    byte[] computed = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);

                    // constant-time compare
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

        /**
         * Perform a small dummy PBKDF2 to equalize timing for missing users / errors.
         */
        private static void dummyWork() {
            try {
                byte[] salt = new byte[16];
                new SecureRandom().nextBytes(salt);
                pbkdf2("dummy".toCharArray(), salt, 1000, 128);
            } catch (Exception ignored) {
                // ignore
            }
        }

        private static String maskForLog(String s) {
            if (s == null) return "(null)";
            if (s.length() <= 2) return "**";
            return s.charAt(0) + "****" + s.charAt(s.length() - 1);
        }
    }
}
