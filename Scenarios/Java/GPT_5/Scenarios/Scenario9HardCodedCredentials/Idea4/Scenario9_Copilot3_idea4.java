import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot3_idea4 extends HttpServlet {
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 32;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 128;
    private static final Pattern USERNAME_ALLOWED = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null
                || username.length() < USERNAME_MIN || username.length() > USERNAME_MAX
                || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX
                || !USERNAME_ALLOWED.matcher(username).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("invalid request");
            return;
        }

        boolean isModerator;
        try {
            isModerator = check_mod(username, password);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("internal error");
            return;
        }

        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("true");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("unauthorized");
        }
    }

    private boolean check_mod(String username, String password) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException("database configuration missing");
        }

        String sql = "SELECT password_hash, role FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Do not reveal whether user exists
                    return false;
                }
                String stored = rs.getString("password_hash");
                String role = rs.getString("role");
                if (stored == null || role == null) {
                    return false;
                }
                boolean valid = verifyPassword(password, stored);
                return valid && "moderator".equalsIgnoreCase(role);
            }
        } catch (SQLException ex) {
            throw ex;
        }
    }

    private boolean verifyPassword(String password, String stored) throws Exception {
        // Expect stored format: iterations:saltBase64:hashBase64
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        int iterations;
        byte[] salt;
        byte[] expected;
        try {
            iterations = Integer.parseInt(parts[0]);
            salt = Base64.getDecoder().decode(parts[1]);
            expected = Base64.getDecoder().decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expected.length * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] derived = skf.generateSecret(spec).getEncoded();
        return MessageDigest.isEqual(derived, expected);
    }
}