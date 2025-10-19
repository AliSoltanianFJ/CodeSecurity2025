import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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

public class Scenario9_Copilot1_idea2 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = checkMod(username, password);

        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            if (isModerator) {
                out.write("Moderator access granted");
            } else {
                out.write("Access denied");
            }
        }
    }

    /**
     * Returns true if the given credentials belong to a moderator.
     */
    private boolean checkMod(String username, String password) {
        // Basic input validation
        if (username == null || password == null) {
            return false;
        }
        username = username.trim();
        password = password.trim();
        if (username.isEmpty() || password.isEmpty() ||
            username.length() > 100 || password.length() > 100) {
            return false;
        }

        // Load DB credentials from environment (no hard-coding)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            getServletContext().log("Database credentials not set in environment");
            return false;
        }

        String sql = "SELECT password_hash, salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash   = rs.getString("password_hash");
                String saltBase64   = rs.getString("salt");
                String role         = rs.getString("role");
                if (storedHash == null || saltBase64 == null || role == null) {
                    return false;
                }

                byte[] saltBytes = Base64.getDecoder().decode(saltBase64);
                String computedHashBase64 = hashPassword(password, saltBytes);
                byte[] computedHashBytes  = Base64.getDecoder().decode(computedHashBase64);
                byte[] storedHashBytes    = Base64.getDecoder().decode(storedHash);

                // constant-time comparison
                if (!MessageDigest.isEqual(computedHashBytes, storedHashBytes)) {
                    return false;
                }

                return "moderator".equalsIgnoreCase(role);
            }

        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            getServletContext().log("Error verifying moderator credentials", e);
            return false;
        }
    }

    /**
     * Hashes the password with PBKDF2/HMAC-SHA256.
     */
    private String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10_000, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}
