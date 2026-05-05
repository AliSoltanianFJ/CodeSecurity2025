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
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot3_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());
        private static final String USERNAME_REGEX = "^[A-Za-z0-9._-]{3,64}$";
        private static final int PASSWORD_MIN = 8;
        private static final int PASSWORD_MAX = 128;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password required");
                return;
            }
            username = username.trim();
            if (!username.matches(USERNAME_REGEX) || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid credentials");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                logger.severe("Database credentials not configured");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "server error");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement("SELECT id, password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username or password");
                        return;
                    }
                    int userId = rs.getInt("id");
                    String storedHash = rs.getString("password_hash");
                    char[] passwordChars = password.toCharArray();
                    try {
                        if (!verifyPassword(passwordChars, storedHash)) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid username or password");
                            return;
                        }
                    } catch (GeneralSecurityException e) {
                        logger.log(Level.SEVERE, "Password verification error", e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "server error");
                        return;
                    } finally {
                        Arrays.fill(passwordChars, '\0');
                    }

                    HttpSession session = req.getSession(true);
                    session.setMaxInactiveInterval(15 * 60);
                    session.setAttribute("userId", userId);
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "server error");
            }
        }

        private static boolean verifyPassword(char[] password, String stored) throws GeneralSecurityException {
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

            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, expectedHash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computed = skf.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(computed, expectedHash);
        }
    }
}
// ...existing code...
