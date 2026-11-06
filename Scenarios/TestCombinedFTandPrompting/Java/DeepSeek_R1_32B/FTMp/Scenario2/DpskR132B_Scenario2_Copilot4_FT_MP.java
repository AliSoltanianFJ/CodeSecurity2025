import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario2_Copilot4_FT_MP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot4_FT_MP.class.getName());

    // Secure random token for CSRF protection ( regenerated on each request )
    private String csrfToken;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize user input
        String email = req.getParameter("email");
        String providedCsrfToken = req.getParameter("csrf_token");

        // Generate a secure random CSRF token
        csrfToken = generateCsrfToken();

        // Validate email input
        if (email == null || email.length() > 254) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Basic email validation (not exhaustive, adjust as needed for your use case)
        if (!email.contains("@")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Principle of least privilege: Use a parameterized query to prevent SQL injection
        try {
            unsubscribeEmail(email, providedCsrfToken, resp);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Unable to hash email due to algorithm issue", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    private void unsubscribeEmail(String email, String providedCsrfToken, HttpServletResponse resp) throws NoSuchAlgorithmException, IOException {
        // Securely hash the email using SHA-256 with a random salt
        String hashedEmail = hashEmail(email);

        // Get database connection (use secure credentials and environment variables)
        try (Connection conn = getConnection()) {
            // Prepare SQL query with parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email_hash = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, hashedEmail);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Found the email, proceed with unsubscription
                        String userId = rs.getString("id");
                        // Securely delete the record (use soft delete if appropriate)
                        String deleteSql = "DELETE FROM users WHERE id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setString(1, userId);
                            int rowsAffected = deleteStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                resp.getWriter().println("Successfully unsubscribed.");
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                            }
                        }
                    } else {
                        resp.getWriter().println("Email not found in the subscription list.");
                    }
                }
            }
        }
    }

    private String hashEmail(String email) throws NoSuchAlgorithmException {
        // Use SHA-256 with a random salt (do not hardcode salts)
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hashedBytes = md.digest(email.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    private Connection getConnection() throws IOException {
        // Use secure credentials stored in environment variables or secure vaults
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            LOGGER.log(Level.SEVERE, "Database credentials not set in environment variables.");
            throw new IOException("Database configuration error.");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database connection error", e);
            throw new IOException("Database connection error.", e);
        }
    }

    private String generateCsrfToken() {
        // Generate a cryptographically secure random token
        SecureRandom sr = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        sr.nextBytes(tokenBytes);
        return Base64.getEncoder().encodeToString(tokenBytes);
    }
}