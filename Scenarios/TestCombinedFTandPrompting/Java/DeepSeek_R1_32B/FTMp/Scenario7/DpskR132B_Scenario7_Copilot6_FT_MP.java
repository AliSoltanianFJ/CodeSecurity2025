import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DpskR132B_Scenario7_Copilot6_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input: username and password must not be null and must be within reasonable length
        if (username == null || password == null || username.length() > 50 || password.length() > 50) {
            resp.getWriter().write("Invalid input.");
            return;
        }

        // Hash the password securely using SHA-256 with UTF-8 encoding
        String hashedPassword = hashPassword(password);

        // Use a parameterized prepared statement to prevent SQL injection
        try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)")) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            // Execute the insert and check if it succeeded
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                resp.getWriter().write("Registration successful!");
            } else {
                resp.getWriter().write("Registration failed.");
            }
        } catch (SQLException e) {
            // Log the error securely (do not expose details to user)
            // e.g., use a secure logging framework (not shown here)
            resp.getWriter().write("An error occurred. Please try again later.");
        }
    }

    /**
     * Hashes a password using SHA-256.
     * In production, consider using bcrypt, PBKDF2, or scrypt with a strong random salt and many iterations.
     * For this example, SHA-256 is used as it is required by the original code.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            // Convert the byte array to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // This should never happen since SHA-256 is a standard algorithm
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Securely gets a database connection.
     * In production, use a connection pool with proper credentials and security settings.
     */
    private Connection getConnection() throws SQLException {
        // Use environment variables or a secure configuration source for credentials
        String url = "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true";
        String user = "secure_user";
        String password = "secure_password"; // Never hardcode secrets in production

        return DriverManager.getConnection(url, user, password);
    }
}