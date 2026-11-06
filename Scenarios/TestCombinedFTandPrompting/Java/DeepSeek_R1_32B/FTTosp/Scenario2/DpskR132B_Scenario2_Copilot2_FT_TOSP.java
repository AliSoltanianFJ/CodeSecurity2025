import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

public class DpskR132B_Scenario2_Copilot2_FT_TOSP extends HttpServlet {

    // Validate email using a regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    // Secure configuration (do not hardcode in real code, use environment variables or secure vaults)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
    private static final String DB_USER = "dbuser"; // Replace with your database username
    private static final String DB_PASSWORD = "dbpassword"; // Replace with your database password

    // Salt for hashing (keep secret in production)
    private static final String SALT = "your-secure-salt";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Ensure HTTPS is enforced in production (configurable)
        if (!isSecureConnection(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Sanitize email to prevent injection attacks
        String sanitizedEmail = sanitizeEmail(email);

        // Get the 'password' parameter (assuming basic auth for unsubscription)
        String providedPassword = req.getParameter("password");
        if (providedPassword == null || providedPassword.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password is required.");
            return;
        }

        // Hash the provided password securely
        String hashedPassword = hashPassword(providedPassword, SALT);

        // Find and unsubscribe the user
        try {
            unsubscribeUser(sanitizedEmail, hashedPassword, resp);
        } catch (SQLException e) {
            // Log exception securely (do not expose details to client)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeUser(String email, String hashedPassword, HttpServletResponse resp) throws SQLException, IOException {
        // Get database connection
        try (Connection conn = getConnection()) {
            // Prepare SQL query with parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email = ? AND password = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                pstmt.setString(2, hashedPassword);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // User found, proceed with unsubscription
                        int userId = rs.getInt("id");
                        // Perform unsubscription (delete or mark as inactive)
                        String deleteSql = "DELETE FROM users WHERE id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, userId);
                            int rowsAffected = deleteStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                resp.getWriter().println("Successfully unsubscribed.");
                                resp.setStatus(HttpServletResponse.SC_OK);
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                            }
                        }
                    } else {
                        // No user found with the given email and password
                        resp.getWriter().println("Email or password incorrect.");
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    }
                }
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private String sanitizeEmail(String email) {
        // Basic sanitization: trim and remove unsafe characters
        return email.trim().replaceAll("[^a-zA-Z0-9._%+-@]", "");
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found.", e);
        }
    }

    private boolean isSecureConnection(HttpServletRequest req) {
        // Check if the request is over HTTPS
        return req.isSecure();
    }
}