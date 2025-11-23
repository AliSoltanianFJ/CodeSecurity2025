import java.io.IOException;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot8_idea4 extends HttpServlet {
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
        if (username.length() < 3 || username.length() > 64 || !username.matches("[A-Za-z0-9_.-]+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        if (password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password");
            return;
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
            return;
        }

        boolean isMod = false;
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            isMod = check_mod(conn, username, password);
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            return;
        }

        resp.setContentType("text/plain");
        resp.getWriter().write(Boolean.toString(isMod));
    }

    private boolean check_mod(Connection conn, String username, String password) {
        String sql = "SELECT password_hash, salt, iterations, role FROM users WHERE username = ? AND role = 'moderator' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String hashB64 = rs.getString("password_hash");
                String saltB64 = rs.getString("salt");
                int iterations = rs.getInt("iterations");
                if (hashB64 == null || saltB64 == null || iterations <= 0) {
                    return false;
                }

                byte[] storedHash = Base64.getDecoder().decode(hashB64);
                byte[] salt = Base64.getDecoder().decode(saltB64);

                byte[] derived = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);
                return MessageDigest.isEqual(storedHash, derived);
            }
        } catch (SQLException | IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
