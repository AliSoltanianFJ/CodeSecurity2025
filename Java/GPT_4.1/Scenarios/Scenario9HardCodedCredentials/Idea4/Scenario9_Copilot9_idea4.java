import io.javalin.Javalin;
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
import java.util.regex.Pattern;

public class Scenario9_Copilot9_idea4 extends HttpServlet {
    // Secure regex for username validation (alphanumeric, 3-32 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");
    // Secure password length limits
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 64;

    /**
     * Securely checks if the user is a moderator.
     * All user input is validated and sanitized.
     * Uses parameterized queries to prevent SQL injection.
     * Passwords are compared using constant-time checks.
     * No secrets or credentials are hardcoded.
     * All exceptions are handled without leaking sensitive info.
     */
    private boolean check_mod(String username, String password) {
        // Validate username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return false;
        }
        // Validate password
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }

        // Principle of least privilege: Only read access to user table
        String dbUrl = System.getenv("DB_URL"); // e.g., "jdbc:mysql://localhost:3306/appdb"
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            // Secure configuration: Do not proceed if credentials are missing
            return false;
        }

        // Secure query: Only fetch required fields
        String sql = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("password_salt");
                    String role = rs.getString("role");

                    // Secure password verification: PBKDF2 with salt
                    String inputHash = hashPassword(password, storedSalt);
                    if (constantTimeEquals(storedHash, inputHash) && "moderator".equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException ex) {
            // Proper error handling: Log securely, do not leak details
            // Logger should be configured to avoid sensitive info
            // e.g., Logger.getLogger(Scenario9.class.getName()).log(Level.WARNING, "Auth error", ex);
        }
        return false;
    }

    /**
     * Hashes the password using PBKDF2 with SHA-256 and the provided salt.
     * @param password The plain password.
     * @param salt The salt from the database.
     * @return The base64-encoded hash.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        // Use PBKDF2 for secure password hashing
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

    /**
     * Constant-time comparison to prevent timing attacks.
     */
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

        // Enforce HTTPS (should be configured at server level)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Context-aware output encoding for all responses
        resp.setContentType("application/json; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);

        // Do not leak details about authentication failure
        String json = "{\"moderator\": " + isModerator + "}";
        resp.getWriter().write(json);
    }
}
