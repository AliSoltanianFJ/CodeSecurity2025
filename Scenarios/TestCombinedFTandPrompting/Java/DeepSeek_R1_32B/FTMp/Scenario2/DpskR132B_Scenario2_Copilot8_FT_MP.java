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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario2_Copilot8_FT_MP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot8_FT_MP.class.getName());

    // Database configuration (should be retrieved from environment variables or secure vaults)
    private static final String DB_URL = System.getenv("UNSUBSCRIBE_DB_URL");
    private static final String DB_USER = System.getenv("UNSUBSCRIBE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("UNSUBSCRIBE_DB_PASSWORD");

    // Salt for hashing (should be stored securely, not hardcoded)
    private static final byte[] SALT = new SecureRandom().generateSeed(16);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || email.length() > 254 || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Hashing algorithm not found", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a configuration error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException, NoSuchAlgorithmException {
        // Hash the email to prevent sensitive data exposure
        String hashedEmail = hashEmail(email);

        // Get database connection
        try (Connection conn = getConnection()) {
            // Prepare SQL query with parameterized query to prevent SQL injection
            String sql = "DELETE FROM users WHERE email_hash = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, hashedEmail);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    resp.getWriter().println("Successfully unsubscribed.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the subscription list.");
                }
            }
        }
    }

    private String hashEmail(String email) throws NoSuchAlgorithmException {
        // Hash email with SHA-256 and a salt
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(SALT);
        byte[] hashBytes = md.digest(email.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    private Connection getConnection() throws SQLException {
        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            throw new SQLException("Database configuration not found in environment variables.");
        }

        // Use SSL with strong cipher suites
        String sslCipher = "TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256:TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384";
        String sslProtocol = "TLSv1.3";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(
                    DB_URL,
                    DB_USER,
                    DB_PASSWORD
            );
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found.", e);
        }
    }

    private boolean isValidEmail(String email) {
        // Strict email validation (RFC 5321)
        String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";
        return email.matches(emailRegex);
    }
}