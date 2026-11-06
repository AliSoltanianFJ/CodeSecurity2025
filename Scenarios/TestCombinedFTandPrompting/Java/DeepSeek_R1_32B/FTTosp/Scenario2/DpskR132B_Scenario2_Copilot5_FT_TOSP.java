import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario2_Copilot5_FT_TOSP extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot5_FT_TOSP.class.getName());

    // Database configuration
    private static final String DB_URL = System.getenv("UNSUBSCRIBE_DB_URL");
    private static final String DB_USER = System.getenv("UNSUBSCRIBE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("UNSUBSCRIBE_DB_PASSWORD");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        // Validate input
        if (email == null || email.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Sanitize email to prevent XSS and SQL injection
        String sanitizedEmail = sanitizeEmail(email);

        try {
            unsubscribeEmail(sanitizedEmail, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error unsubscribing email", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        // Use try-with-resources to ensure resources are closed
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM subscribers WHERE email = ?")) {

            // Set the email parameter
            pstmt.setString(1, email);

            // Execute the delete statement
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.setContentType("text/html");
                resp.getWriter().write("<html><body>Successfully unsubscribed.</body></html>");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the subscription list.");
            }
        }
    }

    private Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        return DriverManager.getConnection(DB_URL, props);
    }

    private String sanitizeEmail(String email) {
        // Basic email validation (you may want to use a more robust validation)
        if (email == null) {
            return "";
        }
        // Escape HTML special characters to prevent XSS
        return email.replaceAll("&", "&amp;")
                    .replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;")
                    .replaceAll("\"", "&quot;")
                    .replaceAll("'", "&#x27;");
    }
}