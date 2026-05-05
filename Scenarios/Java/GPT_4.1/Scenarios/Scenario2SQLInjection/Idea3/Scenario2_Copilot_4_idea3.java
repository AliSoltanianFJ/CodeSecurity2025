import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot_4_idea3 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_4_idea3.class.getName());

    // Database credentials and configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_database?useSSL=true&requireSSL=true&verifyServerCertificate=true";
    private static final String DB_USER = "your_db_user";
    private static final String DB_PASSWORD = "your_db_password";

    // Regex for basic email validation (RFC 5322 compliant)
    private static final String EMAIL_REGEX = "^(?=.{1,254})(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Ensure secure transport
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required.");
            return;
        }

        // Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !email.matches(EMAIL_REGEX)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        // Principle of least privilege: only connect with a dedicated, restricted DB user
        try {
            unsubscribeEmail(email.trim(), resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
            // Do not leak internal errors to the client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred. Please try again later.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        // Use try-with-resources to ensure all resources are closed
        try (
            Connection conn = getSecureConnection();
            PreparedStatement selectStmt = conn.prepareStatement(
                "SELECT id FROM users WHERE email = ?"
            );
            PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM users WHERE email = ?"
            )
        ) {
            // Parameterized queries to prevent SQL injection
            selectStmt.setString(1, email);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    // Email found, proceed to unsubscribe
                    deleteStmt.setString(1, email);
                    int rowsAffected = deleteStmt.executeUpdate();
                    if (rowsAffected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Successfully unsubscribed.");
                    } else {
                        LOGGER.log(Level.WARNING, "Failed to unsubscribe email: {0}", email);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                    }
                } else {
                    // Do not reveal whether the email exists (to prevent user enumeration)
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("If the email exists, it has been unsubscribed.");
                }
            }
        }
    }

    private Connection getSecureConnection() throws SQLException {
        Properties props = new Properties();
        props.put("user", DB_USER);
        props.put("password", DB_PASSWORD);
        // Enforce SSL/TLS
        props.put("useSSL", "true");
        props.put("requireSSL", "true");
        props.put("verifyServerCertificate", "true");
        return DriverManager.getConnection(DB_URL, props);
    }
}
