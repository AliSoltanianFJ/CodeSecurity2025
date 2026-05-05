// ...existing code...
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario9_Copilot6_idea4 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null
                || username.length() < 1 || username.length() > 64
                || password.length() < 1 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        boolean isModerator = check_mod(username, password);
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }
    }

    private boolean check_mod(String username, String password) {
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            return false;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash, salt, iterations, is_moderator FROM users WHERE username = ?")) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHashB64 = rs.getString("password_hash");
                String saltB64 = rs.getString("salt");
                int iterations = rs.getInt("iterations");
                boolean isModerator = rs.getBoolean("is_moderator");

                if (storedHashB64 == null || saltB64 == null || iterations <= 0) {
                    return false;
                }

                byte[] storedHash = Base64.getDecoder().decode(storedHashB64);
                byte[] salt = Base64.getDecoder().decode(saltB64);

                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, storedHash.length * 8);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] derived = skf.generateSecret(spec).getEncoded();
                spec.clearPassword();

                boolean matches = MessageDigest.isEqual(derived, storedHash);

                Arrays.fill(derived, (byte) 0);
                Arrays.fill(salt, (byte) 0);

                return matches && isModerator;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
// ...existing code...
