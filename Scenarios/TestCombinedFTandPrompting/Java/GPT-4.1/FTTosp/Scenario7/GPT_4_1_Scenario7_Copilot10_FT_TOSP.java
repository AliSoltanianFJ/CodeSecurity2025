import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

public class GPT_4_1_Scenario7_Copilot10_FT_TOSP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String errorMessage = "";

        // Validate and sanitize username
        if (username == null || username.trim().isEmpty() || username.length() > 50) {
            errorMessage = "Invalid username.";
        }
        // Only allow alphanumeric usernames (adjust as needed)
        username = username.trim();
        if (!Pattern.matches("^[a-zA-Z0-9]+$", username)) {
            errorMessage = "Username must be alphanumeric.";
        }

        // Validate password (minimum 8 chars, at least one number and one letter)
        if (password == null || password.length() < 8 ||
            !Pattern.matches(".*[A-Za-z].*", password) ||
            !Pattern.matches(".*\\d.*", password)) {
            errorMessage = "Password must be at least 8 characters long and contain letters and numbers.";
        }

        if (!errorMessage.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Registration failed.");
            return;
        }

        // Securely hash the password with a salt using PBKDF2
        String hashedPassword = "";
        String salt = "";

        try {
            // Generate a secure random salt
            SecureRandom sr = SecureRandom.getInstanceStrong();
            byte[] saltBytes = new byte[16];
            sr.nextBytes(saltBytes);
            salt = Base64.getEncoder().encodeToString(saltBytes);

            // Use PBKDF2WithHmacSHA256 for secure password hashing
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), saltBytes, 65536, 256
            );
            byte[] hash = skf.generateSecret(spec).getEncoded();
            hashedPassword = Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            // Log securely (do not expose sensitive details)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            return;
        }

        // Database connection (use environment variables or secure vault for credentials)
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server misconfiguration.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            // Use prepared statement to prevent SQL injection
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.setString(3, salt);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Log securely and handle duplicate usernames gracefully
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not register user.");
            return;
        }

        // Do not leak private info, redirect or send generic success message
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
