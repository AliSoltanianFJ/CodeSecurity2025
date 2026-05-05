import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scenario7_Copilot8_idea4 extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(Scenario7.class);
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_db"; // Replace with your DB URL
    private static final String DB_USER = "your_user"; // Replace with your DB username
    private static final String DB_PASSWORD = "your_password"; // Replace with your DB password

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            register(username, password);
            resp.getWriter().println("User registered successfully!");
        } catch (Exception e) {
            logger.error("Registration failed", e);
            resp.getWriter().println("Registration failed. Please check the logs for details.");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public void register(String username, String password) throws Exception {
        // Validate inputs
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty.");
        }

        // Sanitize inputs (example: prevent XSS)
        username = sanitize(username);

        // Generate a secure salt
        String salt = generateSalt();

        // Hash the password with the salt
        String hashedPassword = hashPassword(password, salt);

        // Store the user in the database
        storeUser(username, hashedPassword, salt);
    }

    private String sanitize(String input) {
        // Implement input sanitization to prevent XSS and other injection attacks.
        // This is a basic example; consider using a robust library like OWASP's Java Encoder.
        return input.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#x27;").replaceAll("/", "&#x2F;");
    }

    private String generateSalt() throws NoSuchAlgorithmException {
        // Use a secure random number generator to create a salt
        SecureRandom random = SecureRandom.getInstanceStrong(); // Strongest available algorithm
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes); // Encode to a string for storage
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        // Use a strong hashing algorithm like SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(salt)); // Use the salt
        byte[] hashedPassword = md.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hashedPassword); // Encode to a string for storage
    }

    private void storeUser(String username, String hashedPassword, String salt) throws SQLException {
        // Use parameterized queries to prevent SQL injection
        String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, salt);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Database error", e);
            throw e; // Re-throw the exception to be handled by the caller
        }
    }
}
