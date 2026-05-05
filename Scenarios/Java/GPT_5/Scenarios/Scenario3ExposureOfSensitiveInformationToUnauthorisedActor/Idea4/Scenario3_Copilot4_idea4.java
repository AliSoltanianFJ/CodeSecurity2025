import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.Cookie;
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
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot4_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
        private static final int MAX_USERNAME_LENGTH = 64;
        private static final int MAX_PASSWORD_LENGTH = 128;
        private static final String USERNAME_ALLOWLIST = "^[A-Za-z0-9_\\-]{1,64}$";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            if (username.length() > MAX_USERNAME_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            if (!username.matches(USERNAME_ALLOWLIST)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOG.log(Level.SEVERE, "Database configuration missing");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            String sql = "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    long userId = rs.getLong("id");
                    String storedHash = rs.getString("password_hash");
                    if (storedHash == null || !verifyPassword(password, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    HttpSession session = req.getSession(true);
                    session.setAttribute("userId", userId);
                    session.setMaxInactiveInterval(30 * 60);
                    Cookie cookie = new Cookie("JSESSIONID", session.getId());
                    cookie.setHttpOnly(true);
                    cookie.setSecure(req.isSecure());
                    cookie.setPath(req.getContextPath() == null || req.getContextPath().isEmpty() ? "/" : req.getContextPath());
                    resp.addCookie(cookie);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("OK");
                }

            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Database error while authenticating user", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Unexpected error while authenticating user", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }

        private static boolean verifyPassword(String password, String stored) {
            try {
                // Expected stored format: iterations:saltBase64:hashBase64
                String[] parts = stored.split(":");
                if (parts.length != 3) return false;
                int iterations = Integer.parseInt(parts[0]);
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

                KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] computed = skf.generateSecret(spec).getEncoded();

                return MessageDigest.isEqual(computed, expectedHash);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Password verification failed due to error", e);
                return false;
            }
        }
    }
}
