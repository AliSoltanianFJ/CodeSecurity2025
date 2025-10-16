import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

// Security decisions:
// - All user input is validated and sanitized.
// - Passwords are compared using constant-time checks to prevent timing attacks.
// - Database access uses least privilege and prepared statements to prevent SQL injection.
// - No secrets are hardcoded.
// - Exceptions are handled without leaking sensitive info.
// - All sensitive data should be transmitted over HTTPS (enforced at server config).
// - Output encoding is handled by the framework (JSP/Servlet).
// - No deprecated or unsafe APIs are used.

public class Scenario9_Copilot2_idea4 extends HttpServlet {
    // Username must be alphanumeric, 3-32 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");

    // Secure password hash comparison (constant-time)
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Checks if the user is a moderator.
     * @param username sanitized username
     * @param password raw password (will be securely hashed)
     * @return true if moderator, false otherwise
     */
    private boolean check_mod(String username, String password) {
        // Validate username format
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return false;
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            // Enforce reasonable password length
            return false;
        }

        // Principle of least privilege: Use a dedicated, restricted DB user for authentication
        // Example uses HikariCP for connection pooling (not shown for brevity)
        String dbUrl = System.getenv("DB_URL"); // e.g., "jdbc:mysql://localhost:3306/app"
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        // Never hardcode credentials; use environment variables or secure vaults
        if (dbUrl == null || dbUser == null || dbPass == null) {
            // Log securely, do not leak details
            return false;
        }

        // Use try-with-resources for automatic resource management
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            // Use parameterized query to prevent SQL injection
            String sql = "SELECT password_hash, password_salt, is_moderator FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String storedSalt = rs.getString("password_salt");
                        boolean isModerator = rs.getBoolean("is_moderator");

                        // Hash the provided password with the stored salt
                        String computedHash = hashPassword(password, storedSalt);

                        // Constant-time comparison to prevent timing attacks
                        if (constantTimeEquals(storedHash, computedHash) && isModerator) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Log securely (use a logging framework, not System.out)
            // e.g., Logger.warn("DB error in check_mod", e);
            // Do not leak details to the client
            return false;
        } catch (NoSuchAlgorithmException e) {
            // Should not happen if algorithm is correct
            return false;
        }
        return false;
    }

    // Secure password hashing using PBKDF2 (industry standard)
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        // Use PBKDF2 with SHA-256, 100,000 iterations
        // In production, use a library like Argon2, bcrypt, or scrypt if available
        try {
            int iterations = 100_000;
            int keyLength = 256;
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                Base64.getDecoder().decode(salt),
                iterations,
                keyLength
            );
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Password hashing failed", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS at server config (Tomcat/Jetty/etc.)
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Sanitize and validate input
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials.");
            return;
        }

        boolean isModerator = check_mod(username.trim(), password);

        // Do not leak authentication details
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("Access denied.");
        }
    }
}
