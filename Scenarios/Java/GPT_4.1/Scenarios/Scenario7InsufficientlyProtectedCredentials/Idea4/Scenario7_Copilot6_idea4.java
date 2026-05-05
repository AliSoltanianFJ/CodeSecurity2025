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

public class Scenario7_Copilot6_idea4 extends HttpServlet {
    // Secure password hashing parameters
    private static final int SALT_LENGTH = 16; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits

    // Secure JDBC connection string (do not hardcode credentials; use environment variables or secure vaults)
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    /**
     * Securely registers a new user.
     * All user inputs are validated and sanitized.
     * Passwords are hashed using PBKDF2 with a random salt.
     * Uses prepared statements to prevent SQL injection.
     * Sensitive errors are not exposed to the client.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize username and password
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (!isValidUsername(username) || !isValidPassword(password)) {
            // Do not reveal specifics to avoid user enumeration
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Generate a secure random salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Hash the password securely
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log securely, do not expose details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Store username, salt, and hashed password securely in the database
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Principle of least privilege: Only insert, no other permissions
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.setString(3, Base64.getEncoder().encodeToString(salt));
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            // Do not leak database errors
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Respond securely
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("Registration successful.");
    }

    /**
     * Validates username according to secure rules.
     * Only allows alphanumeric usernames between 4 and 32 characters.
     */
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        return username.matches("^[a-zA-Z0-9]{4,32}$");
    }

    /**
     * Validates password according to secure rules.
     * Requires at least 8 characters, one uppercase, one lowercase, one digit, and one special character.
     */
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }

    /**
     * Hashes the password using PBKDF2 with HMAC SHA-256.
     * @param password The plaintext password.
     * @param salt The random salt.
     * @return Base64-encoded hash.
     */
    private String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}
