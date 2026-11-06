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


public class GPT_4_1_Scenario7_Copilot3_FT_MP extends HttpServlet {
    // Security decision: Database credentials and secrets must never be hardcoded.
    // Use environment variables or a secure credentials vault for production.
    private static final String DB_URL = System.getenv("SECURE_DB_URL");
    private static final String DB_USER = System.getenv("SECURE_DB_USER");
    private static final String DB_PASS = System.getenv("SECURE_DB_PASS");

    // Security decision: Enforce strong password policy (min length, complexity).
    private static final int MIN_PASSWORD_LENGTH = 12;

    // Security decision: Use a strong, adaptive hash (PBKDF2) with unique salt per user.
    private static final int SALT_LENGTH = 32; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int HASH_LENGTH = 64; // bytes

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Principle of least privilege: Only request required permissions.

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize all user input
        if (!isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }
        if (!isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet security requirements.");
            return;
        }

        // Generate a unique salt for this user
        byte[] salt = new byte[SALT_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
        } catch (NoSuchAlgorithmException e) {
            // Proper error handling: Do not leak sensitive info
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Secure random generation failed.");
            return;
        }

        // Hash the password securely
        String passwordHash;
        try {
            passwordHash = hashPassword(password, salt);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Password hashing failed.");
            return;
        }

        // Clean sensitive data from memory as soon as possible
        wipeCharArray(password.toCharArray());

        // Principle of least privilege: Use read/write only on user table
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
            // Use parameterized queries to prevent SQL injection

            ps.setString(1, username.trim());
            ps.setString(2, passwordHash);
            ps.setString(3, Base64.getEncoder().encodeToString(salt));

            ps.executeUpdate();
            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (SQLIntegrityConstraintViolationException e) {
            // Proper error handling: Do not leak DB details
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }

    // Validate username: allow only alphanumeric, length 3-32
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        if (username.length() < 3 || username.length() > 32) return false;
        return username.matches("^[a-zA-Z0-9]+$");
    }

    // Enforce strong password policy
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < MIN_PASSWORD_LENGTH) return false;
        // At least one uppercase, one lowercase, one digit, one special char
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$");
    }

    // Secure password hashing (PBKDF2)
    private String hashPassword(String password, byte[] salt) throws Exception {
        javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, HASH_ITERATIONS, HASH_LENGTH * 8);
        byte[] hash = skf.generateSecret(spec).getEncoded();
        spec.clearPassword(); // Wipe password from memory
        return Base64.getEncoder().encodeToString(hash);
    }

    // Wipe sensitive data from memory
    private void wipeCharArray(char[] data) {
        if (data == null) return;
        for (int i = 0; i < data.length; i++) data[i] = 0;
    }
}
