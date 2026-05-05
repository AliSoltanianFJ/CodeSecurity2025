import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class Scenario9_Copilot4_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator;
        try {
            isModerator = check_mod(username, password);
        } catch (Exception e) {
            log("Error validating moderator", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        if (isModerator) {
            resp.getWriter().write("Access granted: moderator");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Return true if the given credentials belong to a user
     * whose role is 'moderator'.
     */
    private boolean check_mod(String username, String password)
            throws SQLException, ClassNotFoundException,
                   NoSuchAlgorithmException, InvalidKeySpecException {

        // 1) Validate & sanitize input
        if (username == null || password == null) {
            return false;
        }
        if (username.length() < 3 || username.length() > 50 ||
            password.length() < 8 || password.length() > 100) {
            return false;
        }
        // allow only alphanumeric + underscore
        if (!username.matches("[A-Za-z0-9_]+")) {
            return false;
        }

        // 2) Load DB credentials from environment
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException("Database credentials not set");
        }

        // 3) (Optional) force driver load
        Class.forName("com.mysql.cj.jdbc.Driver");

        // 4) Query user record safely
        String sql = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                String storedSalt = rs.getString("password_salt");
                String role       = rs.getString("role");

                if (!"moderator".equalsIgnoreCase(role)) {
                    return false;
                }
                return verifyPassword(password, storedHash, storedSalt);
            }
        }
    }

    /**
     * Verify a plaintext password against a Base64‐encoded
     * PBKDF2‐SHA256 hash+salt.
     */
    private boolean verifyPassword(String password,
                                   String storedHash,
                                   String storedSalt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] saltBytes = Base64.getDecoder().decode(storedSalt);
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            65536,
            256
        );
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] computedHash = skf.generateSecret(spec).getEncoded();
        byte[] expectedHash = Base64.getDecoder().decode(storedHash);

        // constant‐time compare
        return MessageDigest.isEqual(computedHash, expectedHash);
    }
}
