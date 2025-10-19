// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
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

public class Scenario3_Copilot1_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                // Do not expose details to client
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            // Read DB connection info from environment for safety. Adjust to your environment.
            String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "appuser");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "changeme");

            // Query for stored password hash (expected format: iterations:saltBase64:hashBase64)
            String sql = "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1";

            // Load driver if necessary; modern drivers may auto-register
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.FINE, "MySQL driver not found (may be auto-registered)", e);
            }

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Generic response to avoid user enumeration
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    long userId = rs.getLong("id");
                    String storedHash = rs.getString("password_hash");
                    if (storedHash == null || storedHash.isEmpty()) {
                        // Treat as invalid credentials
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    boolean ok;
                    try {
                        ok = verifyPassword(password, storedHash);
                    } catch (GeneralSecurityException ex) {
                        LOGGER.log(Level.SEVERE, "Password verification failure", ex);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }

                    if (!ok) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }

                    // Successful login: prevent session fixation
                    HttpSession oldSession = req.getSession(false);
                    if (oldSession != null) {
                        try {
                            oldSession.invalidate();
                        } catch (IllegalStateException ignored) {
                        }
                    }
                    HttpSession newSession = req.getSession(true);
                    newSession.setAttribute("userId", userId);
                    newSession.setAttribute("username", username);
                    // secure session settings
                    newSession.setMaxInactiveInterval(30 * 60); // 30 minutes

                    resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    resp.setHeader("Location", "home.jsp");
                }

            } catch (SQLException e) {
                // Log details server-side only
                LOGGER.log(Level.SEVERE, "Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        /**
         * Verify a plaintext password against a stored PBKDF2 hash.
         * Expected stored format: iterations:saltBase64:hashBase64
         */
        private static boolean verifyPassword(String password, String stored) throws GeneralSecurityException {
            if (stored == null) return false;
            String[] parts = stored.split(":");
            if (parts.length != 3) return false;

            int iterations;
            try {
                iterations = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computed = skf.generateSecret(spec).getEncoded();

            return constantTimeArrayEquals(computed, expectedHash);
        }

        private static boolean constantTimeArrayEquals(byte[] a, byte[] b) {
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
// ...existing code...
