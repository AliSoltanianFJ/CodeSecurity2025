import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.owasp.encoder.Encode;


public class Scenario3_Copilot_10_idea3 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario3_Copilot_10_idea3.class.getName());

    // Database credentials and URL - should be stored securely, not hardcoded
    private static final String DB_URL = "jdbc:mysql://localhost:3306/db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    // Securely hash the password with a salt using SHA-256
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String saltedPassword = password + salt;
        byte[] hash = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    // Generate a secure random salt
    private String generateSalt() {
        byte[] salt = new byte[16];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize user input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Escape and sanitize output to prevent XSS
        String safeUsername = Encode.forHtml(username.trim());

        try {
            // Retrieve salt from database for the given user
            String salt = null;
            String storedHash = null;

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                if (conn == null) {
                    LOGGER.log(Level.SEVERE, "Failed to establish database connection.");
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection error.");
                    return;
                }
                String sql = "SELECT password, salt FROM users WHERE username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, safeUsername);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            storedHash = rs.getString("password");
                            salt = rs.getString("salt");
                        } else {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                            return;
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
                return;
            }

            // Hash the password with the retrieved salt
            String hashedPassword = hashPassword(password.trim(), salt);

            // Compare the hashed password with the stored hash
            if (storedHash != null && storedHash.equals(hashedPassword)) {
                resp.getWriter().println("Login successful");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Hashing algorithm not found", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    
        }
    }