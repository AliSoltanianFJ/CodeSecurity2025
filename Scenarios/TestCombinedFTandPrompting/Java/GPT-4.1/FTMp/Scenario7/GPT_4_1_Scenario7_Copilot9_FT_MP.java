import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class GPT_4_1_Scenario7_Copilot9_FT_MP extends HttpServlet {
    // Security decisions:
    // - All user input is validated and sanitized.
    // - Passwords are hashed using PBKDF2WithHmacSHA256 with a unique salt per user.
    // - No secrets or credentials are hardcoded.
    // - Principle of least privilege is assumed for DB account.
    // - Error messages do not leak sensitive info.
    // - Prepared statements prevent SQL injection.
    // - All cryptographic choices follow OWASP recommendations.
    // - Secure logging practices (no sensitive data in logs).
    // - HTTPS must be enforced at the deployment level.
    // - CSRF/XSS protections assumed to be handled globally.
    // - No insecure or deprecated APIs used.
    // - All exceptions are handled securely.

    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario7_Copilot9_FT_MP.class.getName());

    // Strong password policy (should match front-end validation)
    private static final int USERNAME_MIN_LENGTH = 4;
    private static final int USERNAME_MAX_LENGTH = 32;
    private static final int PASSWORD_MIN_LENGTH = 12;
    private static final int PASSWORD_MAX_LENGTH = 128;
    private static final int SALT_LENGTH = 32; // bytes
    private static final int HASH_ITERATIONS = 100_000; // OWASP recommends 100k+ for PBKDF2
    private static final int HASH_LENGTH = 64; // bytes

    private DataSource dataSource; // Should be injected via secure configuration

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize user input
        String username = sanitizeUsername(req.getParameter("username"));
        String password = req.getParameter("password");

        if (!validateUsername(username) || !validatePassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Hash the password securely
        String salt = generateSalt();
        String hash;
        try {
            hash = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            return;
        }

        // Store username, salt, and hash securely
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {

            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("Registration successful. Please login.");

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to register user");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }

    private String sanitizeUsername(String username) {
        if (username == null) return "";
        // Remove leading/trailing whitespace and control chars
        return username.trim().replaceAll("[\\p{Cntrl}]", "");
    }

    private boolean validateUsername(String username) {
        if (username == null) return false;
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) return false;
        // Allow only alphanumeric and limited safe characters
        return username.matches("^[A-Za-z0-9._-]+$");
    }

    private boolean validatePassword(String password) {
        if (password == null) return false;
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) return false;
        // Require uppercase, lowercase, digit, special char (OWASP recommendation)
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c -> "!@#$%^&*()-_=+[]{}|;:',.<>?/".indexOf(c) >= 0);
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private String generateSalt() {
        // Cryptographically secure random salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Use PBKDF2 with HMAC SHA-256, per OWASP recommendations
        char[] passwordChars = password.toCharArray();
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, HASH_ITERATIONS, HASH_LENGTH * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        // Zero password array after use (defense-in-depth)
        java.util.Arrays.fill(passwordChars, '\0');
        return Base64.getEncoder().encodeToString(hash);
    }
}
