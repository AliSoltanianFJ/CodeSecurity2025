import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DpskR132B_Scenario2_Copilot3_FT_TOSP extends HttpServlet {

    // Strict email validation regex (RFC 5321)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}$"
    );

    // Database credentials and connection details (should be sourced from environment variables in production)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mailinglist";
    private static final String DB_USER = "dbuser";
    private static final String DB_PASSWORD = "dbpassword";

    /**
     * Validates and sanitizes the email address.
     * @param email The email address to validate.
     * @return The sanitized email address if valid, null otherwise.
     */
    private String validateEmail(String email) {
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            return null;
        }
        // Basic sanitization (trim whitespace)
        return email.trim();
    }

    /**
     * Hashes the email address using SHA-256.
     * @param email The email address to hash.
     * @return The base64 encoded SHA-256 hash of the email.
     * @throws NoSuchAlgorithmException If SHA-256 algorithm is not found.
     */
    private String hashEmail(String email) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(email.getBytes());
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    /**
     * Unsubscribes the user with the given email address.
     * @param email The email address to unsubscribe.
     * @return true if unsubscription was successful, false otherwise.
     */
    private boolean unsubscribe(String email) {
        String hashedEmail = null;
        try {
            hashedEmail = hashEmail(email);
        } catch (NoSuchAlgorithmException e) {
            // Log exception securely (not shown here)
            return false;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "DELETE FROM subscribers WHERE email_hash = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, hashedEmail);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            // Log exception securely (not shown here)
            return false;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Enforce HTTPS (should be configured at server level in production)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String email = req.getParameter("email");
        String sanitizedEmail = validateEmail(email);

        if (sanitizedEmail == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        boolean success = unsubscribe(sanitizedEmail);
        if (success) {
            resp.getWriter().println("Successfully unsubscribed.");
        } else {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe. Please try again.");
        }
    }
}