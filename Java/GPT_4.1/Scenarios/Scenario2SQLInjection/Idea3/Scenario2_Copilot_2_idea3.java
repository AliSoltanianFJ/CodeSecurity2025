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
import java.util.Properties;

public class Scenario2_Copilot_2_idea3 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_2_idea3.class.getName());

    // Database credentials and connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/newsletter?useSSL=true&requireSSL=true";
    private static final String DB_USER = "readonlyuser";
    private static final String DB_PASSWORD = "strong_readonly_password";

    // Email validation regex (RFC 5322 Official Standard)
    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Sanitize and validate email input
        String email = req.getParameter("email");
        if (email == null || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process your request at this time.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        try (Connection conn = getConnection()) {
            // Use prepared statement to prevent SQL injection
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("You have been unsubscribed successfully.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found in our subscription list.");
                }
            }
        }
    }

    private Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        props.setProperty("useSSL", "true");
        props.setProperty("requireSSL", "true");
        props.setProperty("verifyServerCertificate", "true");
        return DriverManager.getConnection(DB_URL, props);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.length() <= 254 && email.matches(EMAIL_REGEX);
    }
}