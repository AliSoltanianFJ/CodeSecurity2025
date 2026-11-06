import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DpskR132B_Scenario7_Copilot6_FT_TOSP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input: check for null or empty values and reasonable length limits
        if (username == null || username.trim().isEmpty() || username.length() > 50) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }
        if (password == null || password.trim().isEmpty() || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.");
            return;
        }

        // Sanitize username to prevent SQL injection or other attacks
        String sanitizedUsername = sanitizeUsername(username.trim());

        // Hash password using SHA-256
        String hashedPassword = hashPassword(password);

        // Database connection parameters
        String jdbcUrl = "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true";
        String dbUser = "dbuser";
        String dbPassword = "dbpassword";

        try {
            // Get database connection
            Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            // Use prepared statement to prevent SQL injection
            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, sanitizedUsername);
            pstmt.setString(2, hashedPassword);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.getWriter().write("Registration successful!");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register.");
            }
            conn.close();
        } catch (SQLException e) {
            // Log error securely (not shown here), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    // Sanitize username: only allow alphanumeric and underscore
    private String sanitizeUsername(String username) {
        return username.replaceAll("[^A-Za-z0-9_]", "");
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found.", e);
        }
    }
}
