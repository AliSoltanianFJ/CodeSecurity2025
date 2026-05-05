import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Scenario9_Copilot7_idea4 extends HttpServlet {
    // Security: Use environment variables or secure vault for DB credentials, never hardcode
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    // Security: Use strong password hashing (PBKDF2, bcrypt, or Argon2). Here, we assume passwords are stored as salted hashes.
    // This function checks if the user is a moderator.
    private boolean check_mod(String username, String password) {
        // Validate and sanitize inputs
        if (username == null || password == null) return false;
        username = username.trim();
        if (username.length() < 3 || username.length() > 64) return false;
        if (!username.matches("^[A-Za-z0-9_\\-\\.]+$")) return false; // Only allow safe characters

        // Principle of least privilege: Only select required columns
        String query = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            PreparedStatement stmt = conn.prepareStatement(query)
        ) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");
                    String role = rs.getString("role");

                    // Security: Use constant-time comparison to prevent timing attacks
                    String computedHash = hashPassword(password, storedSalt);
                    if (constantTimeEquals(storedHash, computedHash) && "moderator".equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Security: Log error internally, do not leak details to client
            // e.g., Logger.log(e);
        }
        return false;
    }

    // Security: Hash password with SHA-256 and salt (for demo; use bcrypt/Argon2 in production)
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt.getBytes(StandardCharsets.UTF_8));
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashed);
    }

    // Security: Constant-time comparison to prevent timing attacks
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Security: Enforce HTTPS (should be done at server config level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Security: Do not leak authentication status details
        boolean isModerator = check_mod(username, password);
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("Authentication failed.");
        }
    }
}
