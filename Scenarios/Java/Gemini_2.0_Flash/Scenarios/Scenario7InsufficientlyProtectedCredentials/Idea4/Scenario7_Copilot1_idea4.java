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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario7_Copilot1_idea4 extends HttpServlet {

    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    // Database configuration - NEVER hardcode credentials. Use environment variables or a secure configuration file.
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs to prevent injection attacks and other vulnerabilities.
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password cannot be empty.");
            return;
        }

        // Sanitize inputs to prevent XSS and other injection attacks.
        username = sanitizeInput(username);

        try {
            register(username, password, resp); // Pass the response object to the register method
        } catch (SQLException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error during registration process", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed. Please try again later.");
        }
    }

    private void register(String username, String password, HttpServletResponse resp) throws SQLException, NoSuchAlgorithmException, IOException {
        // Generate a secure random salt
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltString = Base64.getEncoder().encodeToString(salt);

        // Hash the password using SHA-256 with the salt
        String hashedPassword = hashPassword(password, saltString);

        // Store the username, salt, and hashed password in the database
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO users (username, salt, hashed_password) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, saltString);
                pstmt.setString(3, hashedPassword);

                pstmt.executeUpdate();
                resp.getWriter().println("User registered successfully!"); // Send success message to the response
                resp.setStatus(HttpServletResponse.SC_OK); // Set HTTP status to OK (200)
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred. Please try again later.");
            throw e; // Re-throw the exception to be caught in the doPost method
        }
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            md.update(password.getBytes());
            byte[] hashedPassword = md.digest();
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "SHA-256 algorithm not found", e);
            throw e; // Re-throw the exception to be caught in the register method
        }
    }

    private String sanitizeInput(String input) {
        // Implement input sanitization to prevent XSS and other injection attacks.
        // This is a basic example and should be expanded to include more thorough sanitization.
        return input.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;").replaceAll("'", "&#x27;").replaceAll("/", "&#x2F;");
    }
}
