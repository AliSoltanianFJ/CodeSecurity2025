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

public class DpskR132B_Scenario7_Copilot10_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input: username and password must not be null or empty
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Enforce reasonable length constraints
        if (username.length() > 50 || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username or password too long.");
            return;
        }

        // Hash password securely using SHA-256 with a random salt
        String hashedPassword = hashPassword(password);

        // Use parameterized query to prevent SQL injection
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO users (username, password) VALUES (?, ?)")) {

            pstmt.setString(1, username.trim());
            pstmt.setString(2, hashedPassword);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                resp.getWriter().write("Registration successful!");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register user.");
            }
        } catch (SQLException e) {
            // Log error securely (not shown), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    private String hashPassword(String password) {
        try {
            // Generate a cryptographically secure random salt
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[32];
            sr.nextBytes(salt);

            // Use SHA-256 hash
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes("UTF-8"));

            // Encode salt and hashed password in Base64
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashedPasswordBase64 = Base64.getEncoder().encodeToString(hashedPassword);

            // Concatenate salt and hashed password with a delimiter
            return saltBase64 + ":" + hashedPasswordBase64;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private Connection getConnection() throws SQLException {
        // Use secure, environment-variable based configuration
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new RuntimeException("Database configuration not found in environment variables.");
        }

        // Use try-with-resources for connection handling
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}