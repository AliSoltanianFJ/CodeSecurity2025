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

public class Scenario7_Copilot6_idea4 extends HttpServlet {

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
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password cannot be empty.");
            return;
        }

        try {
            register(username, password, resp); // Pass the response object to handle responses within the register method
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
        // Ensure database URL, user, and password are not null or empty.
        if (DB_URL == null || DB_URL.trim().isEmpty() || DB_USER == null || DB_USER.trim().isEmpty() || DB_PASSWORD == null || DB_PASSWORD.trim().isEmpty()) {
            logger.severe("Database configuration is incomplete. Check environment variables.");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        // Generate a secure random salt
        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String saltString = Base64.getEncoder().encodeToString(salt);

        // Hash the password using SHA-256 with the salt
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(saltString)); // Use the salt
        byte[] hashedPassword = md.digest(password.getBytes());
        String hashedPasswordString = Base64.getEncoder().encodeToString(hashedPassword);

        // Store the username, salt, and hashed password in the database
        String sql = "INSERT INTO users (username, salt, password) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Use a parameterized query to prevent SQL injection
            pstmt.setString(1, username);
            pstmt.setString(2, saltString);
            pstmt.setString(3, hashedPasswordString);

            pstmt.executeUpdate();

            // Registration successful
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully.");

        } catch (SQLException e) {
            // Handle database errors, log the error, and send an error response
            logger.log(Level.SEVERE, "Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register user due to a database error.");
        }
    }
}
