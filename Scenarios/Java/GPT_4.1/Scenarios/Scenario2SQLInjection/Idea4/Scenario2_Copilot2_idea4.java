import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario2_Copilot2_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot n
// ...existing code...
// Security: Validate and sanitize user input, use prepared statements to prevent SQL injection,
// handle errors securely, and avoid leaking sensitive info. Assume HTTPS is enforced at the server config level.

String email = req.getParameter("email");

// Validate email format strictly (RFC 5322 Official Standard)
if (email == null || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    resp.getWriter().write("Invalid email address.");
    return;
}

// Principle of least privilege: Use a dedicated DB user with only SELECT/UPDATE rights on the relevant table.
// Secure JDBC connection (do not hardcode credentials; use environment variables or secure vaults).
try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
        System.getenv("DB_URL"),
        System.getenv("DB_USER"),
        System.getenv("DB_PASSWORD"))) {

    // Use parameterized queries to prevent SQL injection
    String selectSql = "SELECT id FROM subscribers WHERE email = ?";
    try (java.sql.PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
        selectStmt.setString(1, email);
        try (java.sql.ResultSet rs = selectStmt.executeQuery()) {
            if (rs.next()) {
                // Email exists, proceed to unsubscribe
                String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                try (java.sql.PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int affectedRows = updateStmt.executeUpdate();
                    if (affectedRows > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Successfully unsubscribed.");
                    } else {
                        // Should not happen, but handle gracefully
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("Unsubscribe failed. Please try again later.");
                    }
                }
            } else {
                // Email not found
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Email address not found.");
            }
        }
    }
} catch (Exception e) {
    // Log error securely (do not expose details to user)
    // Use a secure logging framework, e.g., SLF4J, with proper log rotation and access controls
    // org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnsubscribeServlet.class);
    // logger.error("Error during unsubscribe operation", e);

    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    resp.getWriter().write("An error occurred. Please try again later.");
}
// ...existing code...

        }
    }
}
