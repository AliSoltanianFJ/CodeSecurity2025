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

public class Scenario7_Copilot4_idea4 extends HttpServlet {

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
            register(username, password, resp); // Pass HttpServletResponse to handle responses
        } catch (SQLException | NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Registration failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed due to a server error.");
        }
    }

    /**
     * Registers a new user in the database.
     *
     * @param username The username of the new user.
     * @param password The password of the new user.
     * @param resp     The HttpServletResponse object to send responses to the client.
     * @throws SQLException             if a database error occurs.
     * @throws NoSuchAlgorithmException if the hashing algorithm is not available.
     * @throws IOException              if an I/O error occurs.
     */
    private void register(String username, String password, HttpServletResponse resp) throws SQLException, NoSuchAlgorithmException, IOException {
        // Generate a secure salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltString = Base64.getEncoder().encodeToString(salt);

        // Hash the password with the salt
        String hashedPassword = hashPassword(password, salt);

        // Store the username, salt, and hashed password in the database
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             // Use prepared statements to prevent SQL injection
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, salt, password) VALUES (?, ?, ?)")) {

            // Set parameters for the prepared statement
            pstmt.setString(1, username);
            pstmt.setString(2, saltString);
            pstmt.setString(3, hashedPassword);

            // Execute the query
            pstmt.executeUpdate();

            // Registration successful
            resp.getWriter().println("Registration successful!");

        } catch (SQLException e) {
            // Log the error
            logger.log(Level.SEVERE, "Database error", e);

            // Handle specific database errors
            if (e.getSQLState().equals("23000")) {
                // Unique constraint violation (e.g., username already exists)
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
            } else {
                // Generic database error
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred.");
            }
            throw e; // Re-throw the exception to be caught by the caller
        }
    }

    /**
     * Hashes the password using SHA-256 algorithm with a salt.
     *
     * @param password The password to hash.
     * @param salt     The salt to use for hashing.
     * @return The hashed password.
     * @throws NoSuchAlgorithmException if the SHA-256 algorithm is not available.
     */
    private String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Hashing algorithm not available", e);
            throw e; // Re-throw the exception to be caught by the caller
        }
    }
}
