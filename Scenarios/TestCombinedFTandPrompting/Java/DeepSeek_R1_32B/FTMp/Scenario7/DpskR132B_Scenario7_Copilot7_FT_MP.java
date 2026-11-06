import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class DpskR132B_Scenario7_Copilot7_FT_MP extends HttpServlet {
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

        // Enforce reasonable password length
        if (password.length() > 1024) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password is too long.");
            return;
        }

        // Generate a cryptographically secure random salt
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);

        // Hash the password using SHA-256
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Base64 encode the salt and hashed password for storage
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashedPasswordBase64 = Base64.getEncoder().encodeToString(hashedPassword);

            // Use a parameterized query to prevent SQL injection
            String sql = "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username.trim());
                pstmt.setString(2, hashedPasswordBase64);
                pstmt.setString(3, saltBase64);
                pstmt.executeUpdate();
            }

            resp.getWriter().write("Registration successful!");

        } catch (NoSuchAlgorithmException e) {
            // This should never happen since SHA-256 is a valid algorithm
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error hashing password.");
        } catch (SQLException e) {
            // Log the error securely (not shown here), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error storing user data.");
        }
    }

    // Securely get database connection (credentials should be managed externally, not hardcoded)
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true",
            "dbuser",
            "dbpassword"
        );
    }
}