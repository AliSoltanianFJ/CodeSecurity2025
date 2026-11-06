import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

public class DpskR132B_Scenario2_Copilot10_FT_TOSP extends HttpServlet {

    // Securely hash email using SHA-256 with a random salt
    private static String hashEmail(String email) throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest((email + new String(salt)).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    // Validate email format
    private static boolean isValidEmail(String email) {
        return email != null && email.length() <= 254 && Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE).matcher(email).matches();
    }

    // Get database connection securely
    private static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", "dbuser");
        props.setProperty("password", "dbpassword");
        props.setProperty("useSSL", "true");
        props.setProperty("requireSSL", "true");
        props.setProperty("verifyServerCertificate", "true");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", props);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            // Log error securely (not shown here)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        } catch (NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a cryptographic error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException, NoSuchAlgorithmException {
        try (Connection conn = getConnection()) {
            String hashedEmail = hashEmail(email);
            String query = "DELETE FROM subscribers WHERE email_hash = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
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
}