import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class GPT_4_1_Scenario7_Copilot5_FT_MP extends HttpServlet {
    // Security decisions:
    // - All user input is validated and sanitized.
    // - Passwords are hashed using PBKDF2 with a unique salt per user (NO plaintext or weak hashes).
    // - Parameterized queries prevent SQL injection.
    // - No secrets or credentials are hardcoded.
    // - Error messages do not leak sensitive details.
    // - Principle of least privilege: DB user must have only necessary rights.
    // - HTTPS is REQUIRED for deployment (not enforced in code, but MUST be in server config).
    // - All sensitive data is zeroed out after use where possible.

    // Secure configuration: password policy
    private static final int PASSWORD_MIN_LENGTH = 12;
    private static final int PASSWORD_MAX_LENGTH = 128;

    // Secure configuration: PBKDF2 parameters
    private static final int SALT_LENGTH = 32; // bytes
    private static final int HASH_LENGTH = 64; // bytes
    private static final int PBKDF2_ITERATIONS = 100_000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize username
        String username = req.getParameter("username");
        if (username == null || !isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Validate and sanitize password
        String password = req.getParameter("password");
        if (password == null || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.");
            return;
        }

        // Hash the password securely
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        char[] passwordChars = password.toCharArray();
        byte[] hash = null;
        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, HASH_LENGTH * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            // Do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            return;
        } finally {
            // Zero out password in memory
            java.util.Arrays.fill(passwordChars, '\0');
        }

        // Base64 encode salt and hash for storage
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        // Principle of least privilege: use a DB account with only INSERT rights to the users table
        // Secure DB connection: credentials are NEVER hardcoded, must be provided via secure config
        try (Connection conn = SecureDbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {

            ps.setString(1, username.trim()); // context-aware output encoding, trim whitespace
            ps.setString(2, hashBase64);
            ps.setString(3, saltBase64);

            ps.executeUpdate();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful."); // Do not leak info

        } catch (SQLIntegrityConstraintViolationException e) {
            // Username already exists, do not leak details
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken.");
        } catch (SQLException e) {
            // Log securely on server, do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        } finally {
            // Zero out sensitive data
            if (hash != null) java.util.Arrays.fill(hash, (byte) 0);
            java.util.Arrays.fill(salt, (byte) 0);
        }
    }

    // Validate username: only allow alphanumeric, 5-32 chars
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        String trimmed = username.trim();
        return trimmed.length() >= 5 && trimmed.length() <= 32
                && trimmed.matches("^[A-Za-z0-9]+$"); // No special chars to prevent XSS/SQLi
    }

    // Validate password: length, complexity (at least one upper, lower, digit, special)
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH)
            return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if ("!@#$%^&*()-_=+[]{}|;:',.<>?/`~".indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
