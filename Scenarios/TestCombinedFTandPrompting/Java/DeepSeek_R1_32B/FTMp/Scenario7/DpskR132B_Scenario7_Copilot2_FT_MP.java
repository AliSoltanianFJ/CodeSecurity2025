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

public class DpskR132B_Scenario7_Copilot2_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS (principle of secure defaults)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Validate and sanitize user input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Implement input validation
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required fields.");
            return;
        }

        // Define length constraints
        if (username.length() > 50 || password.length() > 1024) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Input exceeds allowed length.");
            return;
        }

        // Validate username format (alphanumeric, no special chars)
        if (!Pattern.matches("^[A-Za-z0-9_]+$", username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
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
            byte[] hashedPassword = md.digest(password.getBytes("UTF-8"));

            // Base64 encode the salt and hashed password for storage
            String saltEncoded = Base64.getEncoder().encodeToString(salt);
            String passwordEncoded = Base64.getEncoder().encodeToString(hashedPassword);

            // Use parameterized queries to prevent SQL injection
            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, passwordEncoded);
                pstmt.setString(3, saltEncoded);
                pstmt.executeUpdate();
            }
            resp.getWriter().write("Registration successful.");

        } catch (NoSuchAlgorithmException | SQLException e) {
            // Log errors securely (not shown here), do not leak details to client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed. Please try again.");
        }
    }

    // Principle of least privilege: use a dedicated database user with minimal rights
    private Connection getConnection() throws SQLException {
        String dbUrl = "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true";
        String dbUser = "db_reg_user";
        String dbPassword = "db_reg_password"; // Never hardcode in production, use environment variables or secure vaults
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}