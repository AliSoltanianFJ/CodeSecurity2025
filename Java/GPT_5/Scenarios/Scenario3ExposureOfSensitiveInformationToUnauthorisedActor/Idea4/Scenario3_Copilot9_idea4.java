// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot9_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final int USERNAME_MIN = 3;
        private static final int USERNAME_MAX = 64;
        private static final int PASSWORD_MIN = 8;
        private static final int PASSWORD_MAX = 128;
        private static final int ITERATIONS = 100_000;
        private static final int KEY_LENGTH = 256; // bits

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            username = username.trim();
            if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX
                    || !username.matches("^[A-Za-z0-9._-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash, password_salt FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        genericInvalid(resp);
                        return;
                    }
                    String storedHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("password_salt");
                    if (storedHashB64 == null || saltB64 == null) {
                        genericInvalid(resp);
                        return;
                    }

                    byte[] salt = Base64.getDecoder().decode(saltB64);
                    byte[] storedHash = Base64.getDecoder().decode(storedHashB64);
                    boolean ok = verifyPassword(password, salt, storedHash);
                    if (!ok) {
                        genericInvalid(resp);
                        return;
                    }

                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    session.setMaxInactiveInterval(30 * 60);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain");
                    resp.getWriter().write("OK");
                }
            } catch (SQLException | GeneralSecurityException ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        private static boolean verifyPassword(String password, byte[] salt, byte[] expected)
                throws GeneralSecurityException {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] derived = skf.generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(derived, expected);
            } finally {
                spec.clearPassword();
            }
        }

        private static void genericInvalid(HttpServletResponse resp) throws IOException {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
        }
    }
}
// ...existing code...
