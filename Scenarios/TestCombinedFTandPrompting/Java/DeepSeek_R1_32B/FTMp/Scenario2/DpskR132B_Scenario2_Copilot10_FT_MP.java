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

public class DpskR132B_Scenario2_Copilot10_FT_MP extends HttpServlet {

    // Secure configuration: Never hardcode secrets, use environment variables or secure vaults
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    // Secure email regex (RFC 5321 pattern)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );

    // Securely hash password using SHA-256 with random salt
    private static String hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(new String(password).getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Password hashing failed", e);
        }
    }

    // Securely generate random salt
    private static byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return salt;
    }

    // Validate and sanitize email input
    private String sanitizeEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return null;
        }
        // Additional sanitization: trim whitespace, normalize case
        return email.trim().toLowerCase();
    }

    // Securely unsubscribe user
    private void unsubscribeUser(String email, char[] password, HttpServletResponse resp) throws SQLException, NoSuchAlgorithmException, IOException {
        // Principle of least privilege: Use a dedicated database user with minimal rights
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Use parameterized query to prevent SQL injection
            String query = "DELETE FROM users WHERE email = ? AND password = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, email);
                // Hash password securely before comparison
                byte[] salt = generateSalt();
                String hashedPassword = hashPassword(password, salt);
                pstmt.setString(2, hashedPassword);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    resp.getWriter().write("Successfully unsubscribed.");
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    // Do not reveal sensitive info (e.g., whether email exists)
                    resp.getWriter().write("Unsubscription failed. Please check your credentials.");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            }
        }
    }

    // Secure error handling
    private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.getWriter().write(message);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize email input
        String email = sanitizeEmail(req.getParameter("email"));
        if (email == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Validate password input
        String password = req.getParameter("password");
        if (password == null || password.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Password is required.");
            return;
        }

        // Principle of least privilege: Never log sensitive data
        // Use secure credential handling
        char[] passwordChars = password.toCharArray();
        try {
            unsubscribeUser(email, passwordChars, resp);
        } catch (SQLException e) {
            // Secure error handling: Do not leak sensitive info
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        } catch (NoSuchAlgorithmException e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }
}