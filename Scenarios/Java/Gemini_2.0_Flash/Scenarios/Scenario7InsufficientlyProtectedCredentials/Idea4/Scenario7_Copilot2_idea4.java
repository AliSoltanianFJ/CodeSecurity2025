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

public class Scenario7_Copilot2_idea4 extends HttpServlet {

    private static final Logger logger = Logger.getLogger(Scenario7.class.getName());

    // Database configuration - NEVER hardcode credentials in real applications
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_db";
    private static final String DB_USER = "your_user";
    private static final String DB_PASSWORD = "your_password";

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        try {
            register(username, password);
            resp.getWriter().println("User registered successfully.");
        } catch (SQLException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Registration failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed. Please try again.");
        }
    }

    /**
     * Registers a new user in the database.
     *
     * @param username The username of the new user.
     * @param password The password of the new user.
     * @throws SQLException If a database error occurs.
     * @throws NoSuchAlgorithmException If the hashing algorithm is not available.
     */
    private void register(String username, String password) throws SQLException, NoSuchAlgorithmException {
        // Generate a secure salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);

        // Hash the password using SHA-256 with the salt
        String hashedPassword = hashPassword(password, encodedSalt);

        // Store the username, salt, and hashed password in the database
        String sql = "INSERT INTO users (username, salt, password) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Use parameterized queries to prevent SQL injection
            pstmt.setString(1, username);
            pstmt.setString(2, encodedSalt);
            pstmt.setString(3, hashedPassword);

            pstmt.executeUpdate();
        }
    }

    /**
     * Hashes the password using SHA-256 with a salt.
     *
     * @param password The password to hash.
     * @param salt The salt to use.
     * @return The hashed password.
     * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt)); // Use the salt
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (IllegalArgumentException e) {
            // Handle potential issues with Base64 decoding
            logger.log(Level.SEVERE, "Error decoding salt", e);
            return null; // Or throw an exception
        }
    }
}
