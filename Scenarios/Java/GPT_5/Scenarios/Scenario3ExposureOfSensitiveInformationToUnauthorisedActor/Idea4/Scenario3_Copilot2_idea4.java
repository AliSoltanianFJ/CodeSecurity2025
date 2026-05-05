import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.SQLException;

public class Scenario3_Copilot2_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());
        private static final int MIN_USERNAME_LEN = 3;
        private static final int MAX_USERNAME_LEN = 64;
        private static final int MIN_PASSWORD_LEN = 8;
        private static final int MAX_PASSWORD_LEN = 128;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
                return;
            }

            username = username.trim();
            if (username.length() < MIN_USERNAME_LEN || username.length() > MAX_USERNAME_LEN
                    || !username.matches("^[A-Za-z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
                return;
            }

            if (password.length() < MIN_PASSWORD_LEN || password.length() > MAX_PASSWORD_LEN) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                logger.severe("Database credentials not configured");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            String stored = null;
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stored = rs.getString(1);
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            if (stored == null || !verifyPassword(password, stored)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setAttribute("username", username);
            session.setMaxInactiveInterval(30 * 60);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("OK");
        }

        private static boolean verifyPassword(String password, String stored) {
            try {
                // expected format: iterations:base64(salt):base64(hash)
                String[] parts = stored.split(":");
                if (parts.length != 3) return false;
                int iterations = Integer.parseInt(parts[0]);
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] hash = Base64.getDecoder().decode(parts[2]);

                KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, hash.length * 8);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] computed = skf.generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(computed, hash);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Password verification failure", e);
                return false;
            }
        }
    }
}
