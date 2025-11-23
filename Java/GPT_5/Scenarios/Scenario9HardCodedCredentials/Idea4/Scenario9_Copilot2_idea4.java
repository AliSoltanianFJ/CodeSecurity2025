// ...existing code...
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.logging.Logger;
import java.util.logging.Level;

// ...existing code...
public class Scenario9_Copilot2_idea4 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario9.class.getName());

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
        if (username.length() < 3 || username.length() > 64 || password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }

        boolean isMod = false;
        try {
            isMod = check_mod(username, password);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication check error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        if (isMod) {
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("true");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }
    }

    private boolean check_mod(String username, String password) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            LOGGER.warning("Database credentials not configured");
            return false;
        }

        String sql = "SELECT password_hash, salt, iterations, role FROM users WHERE username = ?";

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
                String role = rs.getString("role");

                if (storedHashHex == null || saltHex == null || iterations <= 0 || role == null) {
                    return false;
                }

                byte[] storedHash = hexDecode(storedHashHex);
                byte[] salt = hexDecode(saltHex);

                byte[] derived = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);
                if (derived == null) return false;

                boolean matches = MessageDigest.isEqual(storedHash, derived);
                if (!matches) return false;

                return "moderator".equalsIgnoreCase(role) || "mod".equalsIgnoreCase(role);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during authentication", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during authentication", e);
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PBKDF2 failure", e);
            return null;
        }
    }

    private static byte[] hexDecode(String hex) {
        if ((hex.length() & 1) != 0) throw new IllegalArgumentException("Invalid hex string");
        int len = hex.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Invalid hex character");
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
// ...existing code...
