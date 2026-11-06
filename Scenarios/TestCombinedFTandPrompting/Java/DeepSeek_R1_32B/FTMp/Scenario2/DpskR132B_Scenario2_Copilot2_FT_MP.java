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
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DpskR132B_Scenario2_Copilot2_FT_MP extends HttpServlet {

    // Secure configuration: Never hardcode secrets; use environment variables or secure vaults
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
    private static final String DB_USER = System.getenv("DB_USER"); // Get database credentials from environment variables
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    // Secure email validation regex (OWASP-recommended)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
    );

    // Secure hash function for credential storage (do not store plaintext passwords)
    private static final String HASH_ALGORITHM = "SHA-256";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Principle of least privilege: Only allow GET requests from HTTPS (configured at server level)
        // Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Securely hash email (optional: if you store hashed emails)
        // String hashedEmail = hashEmail(email);

        try {
            // Use parameterized query to prevent SQL injection
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            // Secure error handling: Do not leak internal details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "DELETE FROM users WHERE email = ?"
             )) {
            // Parameterize the query to prevent SQL injection
            pstmt.setString(1, email);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.getWriter().println("Successfully unsubscribed.");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the subscription list.");
            }
        }
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Secure hash function implementation (do not store or compare plaintext passwords)
    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(email.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    // Principle of least privilege: Only allow necessary database operations
    // Secure default: Do not allow root or admin credentials
    // Secure configuration: Database credentials should be stored in environment variables or secure vaults
}