import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Scenario3_Copilot1_idea4 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            username = username.trim();
            if (username.length() < 3 || username.length() > 64 || !username.matches("^[A-Za-z0-9_\\-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
                return;
            }

            if (password.length() < 8 || password.length() > 1024) {
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

            PBEKeySpec spec = null;
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash, salt, iterations FROM users WHERE username = ? LIMIT 1")) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        return;
                    }

                    String hashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");
                    if (hashB64 == null || saltB64 == null || iterations <= 0) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        return;
                    }

                    byte[] storedHash;
                    byte[] salt;
                    try {
                        storedHash = Base64.getDecoder().decode(hashB64);
                        salt = Base64.getDecoder().decode(saltB64);
                    } catch (IllegalArgumentException e) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        return;
                    }

                    spec = new PBEKeySpec(password.toCharArray(), salt, iterations, storedHash.length * 8);
                    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                    byte[] computed = skf.generateSecret(spec).getEncoded();

                    if (!MessageDigest.isEqual(computed, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        return;
                    }

                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    session.setMaxInactiveInterval(30 * 60);

                    Cookie cookie = new Cookie("JSESSIONID", session.getId());
                    cookie.setHttpOnly(true);
                    cookie.setSecure(req.isSecure());
                    cookie.setPath(req.getContextPath() == null || req.getContextPath().isEmpty() ? "/" : req.getContextPath());
                    resp.addCookie(cookie);

                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            } catch (SQLException | RuntimeException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } finally {
                if (spec != null) {
                    spec.clearPassword();
                }
            }
        }
    }
}
