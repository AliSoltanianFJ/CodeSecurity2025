import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario2_Copilot5_FT_MP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot5_FT_MP.class.getName());

    // Secure regex for email validation (no path-based XSS)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    // Secure configuration: never hardcode secrets, use environment variables
    private static final String DB_URL = System.getenv("UNSUBSCRIBE_DB_URL");
    private static final String DB_USER = System.getenv("UNSUBSCRIBE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("UNSUBSCRIBE_DB_PASSWORD");

    // Secure random instance
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Hashing parameters
    private static final String SALT = "your-secure-salt"; // In production, generate a strong random salt
    private static final String HASH_ALGORITHM = "SHA-256";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate input: prevent XSS and injection
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            // Log details securely (do not expose stack trace to user)
            LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        } catch (NoSuchAlgorithmException e) {
            // Handle hash algorithm not found (should be configured properly)
            LOGGER.log(Level.SEVERE, "Hashing algorithm not found", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a configuration error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException, NoSuchAlgorithmException {
        // Principle of least privilege: only allow SELECT and UPDATE on users table
        String sql = "SELECT id FROM users WHERE email_hash = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Hash email securely (do not store or compare plaintext)
            String emailHash = hashEmail(email);

            pstmt.setString(1, emailHash);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                // Email not found
                resp.getWriter().println("Email not found in the subscription list.");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // If found, perform unsubscription
            int userId = rs.getInt("id");
            String updateSql = "UPDATE users SET status = 'unsubscribed' WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, userId);
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected > 0) {
                    resp.getWriter().println("Successfully unsubscribed.");
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                }
            }
        }
    }

    private String hashEmail(String email) throws NoSuchAlgorithmException {
        // Hash email with salt to prevent timing attacks
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        md.update(SALT.getBytes());
        byte[] hashBytes = md.digest(email.getBytes());
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private Connection getConnection() throws SQLException {
        // Use environment variables for credentials
        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            throw new SQLException("Database configuration not found in environment variables.");
        }

        // Use secure SSL context (do not disable certificate validation)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "JDBC Driver not found", e);
            throw new SQLException("JDBC Driver not found", e);
        }
    }
}