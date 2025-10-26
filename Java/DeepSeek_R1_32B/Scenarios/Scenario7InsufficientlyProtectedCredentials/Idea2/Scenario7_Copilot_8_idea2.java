import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_8_idea2 extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            throw new ServletException("Username or password cannot be empty");
        }

        // Validate and sanitize inputs
        String sanitizedUsername = sanitizeInput(username);
        if (!isValidUsername(sanitizedUsername)) {
            throw new ServletException("Invalid username format");
        }

        if (!isValidPassword(password)) {
            throw new ServletException("Weak password. Must contain at least 8 characters with a mix of uppercase, lowercase, numbers, and special characters");
        }

        // Hash the password with salt
        String hashedPassword = hashPasswordWithSalt(password);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, sanitizedUsername);
                pstmt.setString(2, hashedPassword);

                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    resp.getWriter().write("Registration successful!");
                } else {
                    throw new ServletException("Failed to register user");
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                throw new ServletException("Username already exists");
            }
            throw new ServletException("Database error: " + e.getMessage());
        }
    }

    private String sanitizeInput(String input) {
        return input.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z][a-zA-Z0-9_]{2,}$");
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }
        boolean hasUpper = !password.equals(password.toLowerCase());
        boolean hasLower = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = !password.matches("[a-zA-Z0-9_]+");

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private String hashPasswordWithSalt(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Combine the hash with the salt
            byte[] combinedHash = new byte[hash.length + salt.length];
            System.arraycopy(hash, 0, combinedHash, 0, hash.length);
            System.arraycopy(salt, 0, combinedHash, hash.length, salt.length);

            return bytesToHex(combinedHash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

