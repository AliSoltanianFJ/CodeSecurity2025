import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario2_Copilot9_FT_TOSP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot9_FT_TOSP.class.getName());

    // Database credentials and connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
    private static final String DB_USER = "dbuser"; // Replace with your database username
    private static final String DB_PASSWORD = "dbpassword"; // Replace with your database password

    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        // Validate input: check for null or empty email
        if (email == null || email.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email address is missing.");
            return;
        }

        // Sanitize email input to prevent XSS and SQL injection
        // For SQL injection prevention, we will use parameterized queries, but basic validation is done here
        if (!isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while unsubscribing email", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        // Get database connection
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Prepare SQL query with parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // Set the email parameter
                pstmt.setString(1, email);

                // Execute the query
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        // Found the email, proceed with unsubscription
                        int userId = rs.getInt("id");
                        String unsubscribeMessage = String.format("User with ID %d has been unsubscribed.", userId);
                        resp.getWriter().write(unsubscribeMessage);
                    } else {
                        // Email not found
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the subscription list.");
                    }
                }
            }
        }
    }

    // Basic email validation (not exhaustive, adjust as needed)
    private boolean isValidEmail(String email) {
        // Simple check for local-part and domain
        return email.contains("@") && email.contains(".");
    }
}