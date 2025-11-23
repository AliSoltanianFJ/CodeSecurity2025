// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Arrays;

public class Scenario3_Copilot8_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final int DEFAULT_ITERATIONS = 100_000;
        private static final int MIN_USERNAME_LEN = 3;
        private static final int MAX_USERNAME_LEN = 64;
        private static final int MIN_PASSWORD_LEN = 8;
        private static final int MAX_PASSWORD_LEN = 256;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            if (username.length() < MIN_USERNAME_LEN || username.length() > MAX_USERNAME_LEN
                    || !username.matches("[A-Za-z0-9._-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            if (password.length() < MIN_PASSWORD_LEN || password.length() > MAX_PASSWORD_LEN) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                LOGGER.log(Level.SEVERE, "Database credentials not configured");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            final String sql = "SELECT id, password_hash, salt, iterations FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    long userId = rs.getLong("id");
                    String hashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");
                    if (iterations <= 0) iterations = DEFAULT_ITERATIONS;

                    if (hashB64 == null || saltB64 == null) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    byte[] expected;
                    byte[] salt;
                    try {
                        expected = Base64.getDecoder().decode(hashB64);
                        salt = Base64.getDecoder().decode(saltB64);
                    } catch (IllegalArgumentException e) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    boolean verified = verifyPassword(password.toCharArray(), salt, iterations, expected);
                    if (!verified) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    HttpSession old = req.getSession(false);
                    if (old != null) {
                        old.invalidate();
                    }
                    HttpSession session = req.getSession(true);
                    session.setAttribute("userId", userId);
                    session.setMaxInactiveInterval(15 * 60);

                    Cookie sid = new Cookie("JSESSIONID", session.getId());
                    sid.setHttpOnly(true);
                    sid.setSecure(req.isSecure());
                    sid.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
                    resp.addCookie(sid);

                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            } catch (SQLException | GeneralSecurityException ex) {
                LOGGER.log(Level.SEVERE, "Authentication failure", ex);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        private static boolean verifyPassword(char[] password, byte[] salt, int iterations, byte[] expected)
                throws GeneralSecurityException {
            try {
                PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, expected.length * 8);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] derived = skf.generateSecret(spec).getEncoded();
                boolean matched = java.security.MessageDigest.isEqual(derived, expected);
                Arrays.fill(derived, (byte) 0);
                spec.clearPassword();
                return matched;
            } finally {
                Arrays.fill(password, '\0');
            }
        }
    }
}
// ...existing code...
