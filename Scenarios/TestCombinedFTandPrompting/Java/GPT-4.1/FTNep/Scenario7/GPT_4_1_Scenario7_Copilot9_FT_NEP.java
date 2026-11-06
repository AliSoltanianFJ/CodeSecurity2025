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

// Principle of least privilege: Use a dedicated DB user with minimal rights for registration.
public class GPT_4_1_Scenario7_Copilot9_FT_NEP extends HttpServlet {
    // Secure password policy constants
    private static final int SALT_LENGTH = 16; // bytes
    private static final int HASH_LENGTH = 64; // bytes
    private static final int PBKDF2_ITERATIONS = 310000;

    // Secure DB credentials are read from environment variables, not hardcoded.
    private static final String DB_URL = System.getenv("SECURE_DB_URL");
    private static final String DB_USER = System.getenv("SECURE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("SECURE_DB_PASSWORD");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Validate and sanitize user input
        String username = sanitize(req.getParameter("username"));
        String password = req.getParameter("password");

        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Registration failed."); // Generic message, no details leaked
            return;
        }

        try {
            // Generate salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);

            // Hash password with PBKDF2
            char[] passwordChars = password.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, HASH_LENGTH * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            Base64.Encoder enc = Base64.getEncoder();
            String saltBase64 = enc.encodeToString(salt);
            String hashBase64 = enc.encodeToString(hash);

            // Clear sensitive data from memory
            spec.clearPassword();
            java.util.Arrays.fill(passwordChars, '\0');
            password = null;

            // Securely store username, salt, and hash
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    stmt.setString(2, hashBase64);
                    stmt.setString(3, saltBase64);
                    stmt.executeUpdate();
                }
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration succeeded."); // No sensitive info leaked
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
            // Log error securely (not shown here), never leak details to client
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Registration failed."); // Generic error message
        }
    }

    // Basic input validation and sanitization (avoid XSS/SQLi)
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>\"'%;()&+]", "");
    }

    private boolean isValidUsername(String username) {
        // Username: 3-32 chars, alphanumeric only
        return username != null && username.matches("^[a-zA-Z0-9]{3,32}$");
    }

    private boolean isValidPassword(String password) {
        // Password policy: min 12 chars, at least one upper, lower, digit, special
        return password != null
                && password.length() >= 12
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[^a-zA-Z0-9].*");
    }
}
