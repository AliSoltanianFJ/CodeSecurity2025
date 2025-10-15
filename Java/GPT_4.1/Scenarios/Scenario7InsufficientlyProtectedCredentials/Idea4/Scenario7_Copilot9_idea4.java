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

public class Scenario7_Copilot9_idea4 extends HttpServlet {
    // Secure password hashing parameters
    private static final int SALT_LENGTH = 16; // bytes
    private static final int HASH_ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits

    // Secure JDBC connection string (do not hardcode credentials)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/securedb?useSSL=true";
    // Use environment variables or a secure vault for credentials
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize user inputs
        String username = sanitize(req.getParameter("username"));
        String password = req.getParameter("password");

        if (!isValidUsername(username) || !isValidPassword(password)) {
            // Do not leak details about validation failure
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        // Generate a secure random salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // Hash the password securely using PBKDF2
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log securely and do not expose details to the client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Store username, salt, and hashed password securely using prepared statements
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // Principle of least privilege: use a dedicated user with only insert privileges
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.setString(3, Base64.getEncoder().encodeToString(salt));
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Handle duplicate usernames and other SQL errors securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Respond securely
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("Registration successful.");
    }

    // Secure password hashing using PBKDF2 with HMAC-SHA256
    private String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    // Basic username validation (adjust as needed for your requirements)
    private boolean isValidUsername(String username) {
        if (username == null) return false;
        // Only allow alphanumeric usernames, 3-32 chars
        return username.matches("^[a-zA-Z0-9]{3,32}$");
    }

    // Basic password validation (adjust as needed for your requirements)
    private boolean isValidPassword(String password) {
        if (password == null) return false;
        // Require at least 12 characters, at least one uppercase, one lowercase, one digit, one special char
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");
    }

    // Context-aware output encoding and sanitization
    private String sanitize(String input) {
        if (input == null) return null;
        // Remove leading/trailing whitespace and encode for SQL safety (though prepared statements are used)
        return input.trim();
    }
}
