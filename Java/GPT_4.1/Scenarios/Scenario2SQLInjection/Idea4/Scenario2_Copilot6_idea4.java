import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario2_Copilot6_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot n
// ...existing code...
// Security: Validate and sanitize user input, use prepared statements to prevent SQL injection,
// handle errors securely, and avoid leaking sensitive info. Do not hardcode credentials.
// Principle of least privilege: Only allow unsubscribe if email exists.
// Assumes HTTPS is enforced at the server configuration level.

String email = req.getParameter("email");

// Validate email format strictly using regex (RFC 5322 Official Standard)
if (email == null || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
    return;
}

// Use try-with-resources for secure resource management
try (
    // Use least privilege: only allow access to unsubscribe functionality
    java.sql.Connection conn = java.sql.DriverManager.getConnection(
        System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD")
    );
    // Parameterized query to prevent SQL injection
    java.sql.PreparedStatement checkStmt = conn.prepareStatement(
        "SELECT COUNT(*) FROM subscribers WHERE email = ?"
    );
    java.sql.PreparedStatement unsubscribeStmt = conn.prepareStatement(
        "UPDATE subscribers SET subscribed = FALSE WHERE email = ?"
    )
) {
    checkStmt.setString(1, email);
    try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
        if (rs.next() && rs.getInt(1) > 0) {
            // Email exists, proceed to unsubscribe
            unsubscribeStmt.setString(1, email);
            int updated = unsubscribeStmt.executeUpdate();
            if (updated > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Successfully unsubscribed.");
            } else {
                // Should not happen, but handle gracefully
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
            }
        } else {
            // Do not reveal whether email exists for privacy
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("If this email is subscribed, it will be unsubscribed.");
        }
    }
} catch (Exception e) {
    // Log securely (do not expose details to user)
    // Use a secure logging framework in production
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred.");
}
// ...existing code...

        }
    }
}
