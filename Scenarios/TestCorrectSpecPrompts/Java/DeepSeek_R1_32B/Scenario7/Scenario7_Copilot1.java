import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

public class Scenario7_Copilot1 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing username or password");
            return;
        }

        // Generate random salt
        byte[] salt = new byte[16];
        try {
            SecureRandom.getInstanceStrong().nextBytes(salt);
        } catch (NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Hashing error");
            System.err.println("SecureRandom error: " + e.getMessage());
            return;
        }

        // Derive hashed password using PBKDF2
        String hashedPassword;
        try {
            hashedPassword = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Hashing error");
            System.err.println("Hashing error: " + e.getMessage());
            return;
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUsername = System.getenv("DB_USERNAME");
        String dbPassword = System.getenv("DB_PASSWORD");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
            // Check if username already exists
            String checkQuery = "SELECT username FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                        resp.getWriter().write("Username already exists");
                        return;
                    }
                }
            }

            // Insert new user
            String insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.setBytes(3, salt);
                insertStmt.executeUpdate();
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error");
            // Log the error
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        int iterations = 100000; // PBKDF2 iterations
        int keyLength = 256;
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        // Convert to Base64 for storage
        return Base64.getEncoder().encodeToString(hash);
    }
}
