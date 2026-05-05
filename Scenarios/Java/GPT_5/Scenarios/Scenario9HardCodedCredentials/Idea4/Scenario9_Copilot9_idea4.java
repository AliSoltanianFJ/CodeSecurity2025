// ...existing code...
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot9_idea4 extends HttpServlet {
    private static final Pattern USERNAME_ALLOW = Pattern.compile("^[A-Za-z0-9_.-]{3,64}$");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 128;
    private static final int DEFAULT_ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        if (!USERNAME_ALLOW.matcher(username).matches() ||
                password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }

        boolean isMod = check_mod(username, password);
        if (isMod) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }
    }

    private boolean check_mod(String username, String password) {
        if (username == null || password == null) return false;
        if (!USERNAME_ALLOW.matcher(username).matches()) return false;
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) return false;

        String jdbcUrl = System.getenv("JDBC_URL");
        String dbUser = System.getenv("JDBC_USER");
        String dbPass = System.getenv("JDBC_PASSWORD");
        if (jdbcUrl == null || dbUser == null || dbPass == null) return false;

        byte[] fallbackSalt = new byte[16];
        new SecureRandom().nextBytes(fallbackSalt);

        String sql = "SELECT password_hash, salt, iterations, role FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // perform fake hash to mitigate timing attacks
                    derivePbkdf2(password.toCharArray(), fallbackSalt, DEFAULT_ITERATIONS, KEY_LENGTH);
                    return false;
                }

                String storedHashB64 = rs.getString("password_hash");
                String storedSaltB64 = rs.getString("salt");
                int iterations = rs.getInt("iterations");
                if (rs.wasNull()) iterations = DEFAULT_ITERATIONS;
                String role = rs.getString("role");

                if (storedHashB64 == null || storedSaltB64 == null) {
                    derivePbkdf2(password.toCharArray(), fallbackSalt, iterations, KEY_LENGTH);
                    return false;
                }

                byte[] storedHash;
                byte[] salt;
                try {
                    storedHash = Base64.getDecoder().decode(storedHashB64);
                    salt = Base64.getDecoder().decode(storedSaltB64);
                } catch (IllegalArgumentException e) {
                    derivePbkdf2(password.toCharArray(), fallbackSalt, iterations, KEY_LENGTH);
                    return false;
                }

                byte[] derived = derivePbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);
                boolean matches = MessageDigest.isEqual(derived, storedHash);

                return matches && role != null && "moderator".equalsIgnoreCase(role);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derivePbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, Math.max(iterations, 1), keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
// ...existing code...
