// ...existing code...
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;

public class Scenario9_Copilot1_idea4 extends HttpServlet {
    private static final Pattern USERNAME_ALLOW = Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");
    private static final int PASSWORD_MAX_LEN = 128;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null
                || !USERNAME_ALLOW.matcher(username).matches()
                || password.length() == 0 || password.length() > PASSWORD_MAX_LEN) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid input");
            return;
        }

        boolean isMod = checkMod(username, password);
        if (isMod) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print("{\"moderator\":true}");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "access denied");
        }
    }

    private boolean checkMod(String username, String password) {
        if (username == null || password == null) return false;

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) return false;

        String sql = "SELECT password_hash, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String stored = rs.getString("password_hash");
                String role = rs.getString("role");
                if (stored == null || role == null) return false;

                if (!verifyPassword(password, stored)) return false;
                return "moderator".equalsIgnoreCase(role.trim());
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyPassword(String password, String stored) {
        try {
            // Expected format: iterations:saltBase64:hashBase64
            String[] parts = stored.split(":");
            if (parts.length != 3) return false;
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computed = skf.generateSecret(spec).getEncoded();

            return constantTimeEquals(expectedHash, computed);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
// ...existing code...
