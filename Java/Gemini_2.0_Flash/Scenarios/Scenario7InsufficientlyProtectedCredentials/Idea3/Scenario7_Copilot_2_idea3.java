import java.security.MessageDigest; // Kept as requested, though PBKDF2 is used below
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot_2_idea3 extends HttpServlet {

    // --- Configuration Constants ---
    
    // Database configuration (Replace with actual secure configuration)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/appdb?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "dbuser";
    private static final String DB_PASSWORD = "dbpassword";

    // PBKDF2 Configuration for password hashing (Strong recommendation: 100,000+ iterations)
    private static final int ITERATIONS = 150000; 
    private static final int SALT_LENGTH = 16;     // 16 bytes = 128 bits
    private static final int KEY_LENGTH = 256;     // Output hash length
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // --- Helper Methods for Secure Hashing ---

    /**
     * Generates a cryptographically secure random salt.
     */
    private byte[] generateSalt() throws Exception {
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes the password using PBKDF2.
     */
    private String hashPassword(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        byte[] hash = skf.generateSecret(spec).getEncoded();
        // Return the hash base64 encoded for storage
        return Base64.getEncoder().encodeToString(hash);
    }

    // --- Registration Logic ---

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // -copilot next line-

        // 1. Basic Input Validation
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must be provided.");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            // 2. Security Step: Hash the password securely
            byte[] salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            String saltString = Base64.getEncoder().encodeToString(salt);

            // 3. Database Connection and Prepared Statement (SQL Injection prevention)
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // SQL: Ensure your 'users' table has columns: username, password_hash, salt, iterations
            String sql = "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            
            // Set parameters securely
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, saltString);
            pstmt.setInt(4, ITERATIONS);

            // 4. Execute and Respond
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.setContentType("text/plain");
                resp.getWriter().write("User registration successful for: " + username);
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to unknown database issue.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            // Handle case where username already exists
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken.");
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            // In a real application, log this error securely
            throw new ServletException("Database access error.", e);
        } catch (Exception e) {
            // Catches exceptions from hashing/salt generation
            System.err.println("Security or internal error: " + e.getMessage());
            throw new ServletException("Internal server error during user creation.", e);
        } finally {
            // 5. Resource cleanup
            try { 
                if (pstmt != null) pstmt.close(); 
            } catch (SQLException ignore) {}
            try { 
                if (conn != null) conn.close(); 
            } catch (SQLException ignore) {}
        }
    }
}