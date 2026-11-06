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
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario2_Copilot6_FT_MP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario2_Copilot6_FT_MP.class.getName());

    // Database credentials and connection details
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER"); // Read from environment variables
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD"); // Read from environment variables

    // Secure email pattern (RFC 5321)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Ensure HTTPS is used (should be configured at server level, but enforce here as a safeguard)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        String email = req.getParameter("email");

        // Validate and sanitize email input
        if (email == null || email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        // Get database connection
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {

            // Set the email parameter safely
            pstmt.setString(1, email);

            // Check if the email exists in the database
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    unsubscribeUser(userId, conn, resp);
                } else {
                    resp.getWriter().println("Email not found in the subscription list.");
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }
    }

    private void unsubscribeUser(int userId, Connection conn, HttpServletResponse resp) throws SQLException, IOException {
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            pstmt.setInt(1, userId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.getWriter().println("Successfully unsubscribed.");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.getWriter().println("Failed to unsubscribe.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}