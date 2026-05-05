import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot4_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }
        username = username.trim();
        if (username.length() < 3 || username.length() > 64 || !username.matches("^[A-Za-z0-9_\\-\\.]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }
        if (password.length() < 8 || password.length() > 1024) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password");
            return;
        }

        boolean isMod = check_mod(username, password);

        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter w = resp.getWriter()) {
            w.print(isMod ? "true" : "false");
        }
    }

    private boolean check_mod(String username, String password) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            return false;
        }

        String sql = "SELECT password_hash, salt, iterations, is_moderator FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHashHex = rs.getString("password_hash");
                String saltHex = rs.getString("salt");
                int iterations = rs.getInt("iterations");
                boolean isModerator = rs.getBoolean("is_moderator");

                if (!isModerator) {
                    return false;
                }
                if (storedHashHex == null || saltHex == null || iterations <= 0) {
                    return false;
                }

                byte[] salt = hexToBytes(saltHex);
                byte[] storedHash = hexToBytes(storedHashHex);
                byte[] derived = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);
                if (derived == null) {
                    return false;
                }
                return constantTimeEquals(storedHash, derived);
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return f.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static byte[] hexToBytes(String s) {
        if (s == null) return null;
        int len = s.length();
        if ((len & 1) != 0) return null;
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
