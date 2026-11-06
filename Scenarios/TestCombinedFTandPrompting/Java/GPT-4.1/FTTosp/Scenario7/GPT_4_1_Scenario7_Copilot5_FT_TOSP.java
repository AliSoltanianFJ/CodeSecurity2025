import java.security.MessageDigest;
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

public class GPT_4_1_Scenario7_Copilot5_FT_TOSP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize username
        if (username == null || username.length() < 5 || username.length() > 50 ||
                !Pattern.matches("^[a-zA-Z0-9._-]+$", username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }
        username = username.trim();

        // Validate password (at least 12 chars, includes upper, lower, digit, symbol)
        if (password == null || password.length() < 12 ||
                !Pattern.matches(".*[A-Z].*", password) ||
                !Pattern.matches(".*[a-z].*", password) ||
                !Pattern.matches(".*\\d.*", password) ||
                !Pattern.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\|,.<>/?].*", password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet complexity requirements.");
            return;
        }

        // Hash password with salt using PBKDF2 (stronger than SHA-256)
        String salt = generateSalt();
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Password hashing failed.");
            return;
        }

        // Clear sensitive data from memory
        password = null;

        // Database connection (use environment variables or secure vault for credentials)
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration missing.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username); // already validated and sanitized
                stmt.setString(2, hashedPassword);
                stmt.setString(3, salt);
                stmt.executeUpdate();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }

    private String generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(),
                    Base64.getDecoder().decode(salt),
                    100_000,
                    256
            );
            byte[] hash = skf.generateSecret(spec).getEncoded();
            // Zero out the char[] password as soon as possible
            java.util.Arrays.fill(spec.getPassword(), '\0');
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Hashing failed", e);
        }
    }
}
