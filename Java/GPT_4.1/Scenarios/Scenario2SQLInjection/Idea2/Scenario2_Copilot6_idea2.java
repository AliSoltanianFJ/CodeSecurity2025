import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario2_Copilot6_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot n
// ...existing code...
String email = req.getParameter("email");

// Validate and sanitize input
if (email == null || email.isEmpty() || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
    return;
}

// Use try-with-resources for safe resource management
try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
        "jdbc:mysql://localhost:3306/yourdb", "username", "password");
     java.sql.PreparedStatement checkStmt = conn.prepareStatement(
        "SELECT COUNT(*) FROM subscribers WHERE email = ?")) {

    checkStmt.setString(1, email);
    try (java.sql.ResultSet rs = checkStmt.executeQuery()) {
        if (rs.next() && rs.getInt(1) > 0) {
            // Email exists, proceed to unsubscribe
            try (java.sql.PreparedStatement deleteStmt = conn.prepareStatement(
                    "DELETE FROM subscribers WHERE email = ?")) {
                deleteStmt.setString(1, email);
                int affected = deleteStmt.executeUpdate();
                if (affected > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                }
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
        }
    }
} catch (Exception e) {
    // Log exception securely (do not expose details to user)
    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
}
// ...existing code...

        }
    }
}
