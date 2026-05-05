import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot7_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Pattern USERNAME_ALLOWED = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
        private static final int MIN_PASSWORD_LEN = 8;
        private static final int MAX_PASSWORD_LEN = 128;
        private static final int DEFAULT_ITERATIONS = 100_000;
        private static final int DERIVED_KEY_LENGTH = 32; // 256 bits

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null
                    || !USERNAME_ALLOWED.matcher(username).matches()
                    || password.length() < MIN_PASSWORD_LEN
                    || password.length() > MAX_PASSWORD_LEN) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash_hex, salt_hex, iterations FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }
                    String storedHashHex = rs.getString("password_hash_hex");
                    String saltHex = rs.getString("salt_hex");
                    int iterations = rs.getInt("iterations");
                    if (rs.wasNull() || storedHashHex == null || saltHex == null) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }
                    if (iterations <= 0) iterations = DEFAULT_ITERATIONS;

                    byte[] storedHash = hexToBytes(storedHashHex);
                    byte[] salt = hexToBytes(saltHex);

                    byte[] computed = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length);
                    if (!constantTimeEquals(storedHash, computed)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    HttpSession old = req.getSession(false);
                    if (old != null) old.invalidate();
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    session.setMaxInactiveInterval(15 * 60);

                    String cookie = "JSESSIONID=" + session.getId()
                            + "; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=" + session.getMaxInactiveInterval();
                    resp.setHeader("Set-Cookie", cookie);
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            } catch (SQLException | GeneralSecurityException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
            }
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int dkLen) throws GeneralSecurityException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, dkLen * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        private static byte[] hexToBytes(String hex) throws IllegalArgumentException {
            if (hex.length() % 2 != 0) throw new IllegalArgumentException("Invalid hex length");
            int len = hex.length() / 2;
            byte[] out = new byte[len];
            for (int i = 0; i < len; i++) {
                int hi = Character.digit(hex.charAt(i * 2), 16);
                int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
                if (hi < 0 || lo < 0) throw new IllegalArgumentException("Invalid hex");
                out[i] = (byte) ((hi << 4) + lo);
            }
            return out;
        }

        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            return MessageDigest.isEqual(a, b);
        }
    }
}
