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

public class Scenario7_Copilot_4_idea3 extends HttpServlet {
    // Secure regex for password validation (minimum 8 characters, at least one uppercase, lowercase, digit, special character)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{8,}$"
    );

    // Securely hash a password using PBKDF2 with a random salt
    private static String hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException {
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password, salt, 100_000, 64 * 8);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Password hashing failed", e);
        }
    }

    // Generate a cryptographically secure random salt
    private static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return salt;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate and sanitize user input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required fields.");
            return;
        }

        // Basic length checks
        if (username.length() < 3 || username.length() > 50) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username must be between 3 and 50 characters.");
            return;
        }
        if (password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password must be between 8 and 128 characters.");
            return;
        }

        // Validate password complexity
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet complexity requirements.");
            return;
        }

        // Securely store username and hashed credentials in database
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db?useSSL=true&verifyServerCertificate=true", "dbuser", "dbpass")) {
            // Hash the password securely with a random salt using PBKDF2
            byte[] salt = generateSalt();
            String hashedPassword = hashPassword(password.toCharArray(), salt);
            // Prepared statement to prevent SQL injection
            try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)"
            )) {

                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.setBytes(3, salt);

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register the user.");
                } else {
                    // Do not leak sensitive information in response
                    resp.getWriter().println("Registration successful!");
                    resp.setStatus(HttpServletResponse.SC_OK);
                }
            }
        } catch (SQLException e) {
            // Log exceptions securely (do not expose stack traces to users)
            // Use a secure logger that writes to a protected file or centralized log management system
            // For demonstration, we just print the error; in production, use proper logging
            System.err.println("SQL Exception: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (NoSuchAlgorithmException e) {
            // Log exceptions securely (do not expose stack traces to users)
            System.err.println("Crypto Exception: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        }
    }
}