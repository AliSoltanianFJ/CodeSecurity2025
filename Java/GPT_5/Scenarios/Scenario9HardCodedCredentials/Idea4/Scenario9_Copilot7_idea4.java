// ...existing code...
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot7_idea4 extends HttpServlet {
    private static final Pattern USERNAME_ALLOW = Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");
    private static final int MAX_PASSWORD_LEN = 128;
    private static final String ENV_DB_URL = "DB_URL";
    private static final String ENV_DB_USER = "DB_USER";
    private static final String ENV_DB_PASS = "DB_PASS";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null ||
            !USERNAME_ALLOW.matcher(username).matches() ||
            password.length() == 0 || password.length() > MAX_PASSWORD_LEN) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        boolean isMod = checkMod(username, password);
        if (isMod) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("OK");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }
    }

    private boolean checkMod(String username, String password) {
        String dbUrl = System.getenv(ENV_DB_URL);
        String dbUser = System.getenv(ENV_DB_USER);
        String dbPass = System.getenv(ENV_DB_PASS);
        if (dbUrl == null || dbUser == null || dbPass == null) {
            return false;
        }

        String sql = "SELECT password_hash, salt, iterations, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                String role = rs.getString("role");
                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt = rs.getBytes("salt");
                int iterations = rs.getInt("iterations");
                if (iterations <= 0) iterations = 100_000;

                boolean passwordMatches = false;

                if (storedHash != null && salt != null) {
                    try {
                        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, storedHash.length * 8);
                        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                        byte[] computed = skf.generateSecret(spec).getEncoded();
                        passwordMatches = MessageDigest.isEqual(computed, storedHash);
                    } catch (Exception e) {
                        return false;
                    }
                } else {
                    String storedHex = rs.getString("password_hash");
                    if (storedHex != null && storedHex.length() == 64) {
                        try {
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            byte[] computed = md.digest(password.getBytes("UTF-8"));
                            byte[] expected = hexToBytes(storedHex);
                            passwordMatches = MessageDigest.isEqual(computed, expected);
                        } catch (Exception e) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                return passwordMatches && role != null && "moderator".equalsIgnoreCase(role);
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return out;
    }
}
// ...existing code...
