import io.javalin.Javalin;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot8_idea4 extends HttpServlet {
    // Secure password hashing parameters
    private static final int SALT_LENGTH = 16; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits

    /**
     * Securely hashes a password using PBKDF2 with a random salt.
     * @param password The password to hash.
     * @param salt The salt to use.
     * @return The hashed password as a Base64 string.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private String hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Use PBKDF2 with HMAC SHA-256 for strong password hashing
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Generates a cryptographically secure random salt.
     * @return The salt as a byte array.
     */
    private byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    /**
     * Sanitizes and validates a username.
     * Only allows alphanumeric characters and underscores, length 3-32.
     * @param username The username to validate.
     * @return true if valid, false otherwise.
     */
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        return username.matches("^[A-Za-z0-9_]{3,32}$");
    }

    /**
     * Validates password strength.
     * Minimum 8 characters, at least one uppercase, one lowercase, one digit, one special character.
     * @param password The password to validate.
     * @return true if valid, false otherwise.
     */
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (should be configured at server level, but check here as defense-in-depth)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        // Retrieve and sanitize user input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs
        if (!isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }
        if (!isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet complexity requirements.");
            return;
        }

        // Hash password securely
        byte[] salt = generateSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password.toCharArray(), salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log error securely (do not leak details to user)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Principle of least privilege: Use a dedicated DB user with minimal permissions
        // Never hardcode credentials; use environment variables or secure vaults
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        // Use parameterized queries to prevent SQL injection
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, saltBase64);

            int rows = stmt.executeUpdate();
            if (rows == 1) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("User registered securely.");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
            }
        } catch (SQLException e) {
            // Do not leak DB errors to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
        }
    }
}
