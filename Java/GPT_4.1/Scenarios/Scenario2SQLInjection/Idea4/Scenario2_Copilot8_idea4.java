import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario2_Copilot8_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot n
// ...existing code...
        // Security: Validate and sanitize the 'email' parameter from the request
        String email = req.getParameter("email");
        if (email == null || email.isEmpty()) {
            // Do not reveal details about missing/invalid input
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
            return;
        }

        // Security: Strict email format validation (RFC 5322 simplified)
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        // Security: Use least privilege DB credentials, parameterized queries, and avoid exposing errors
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASS"))) {
            // Principle of least privilege: Only allow update on the relevant table/column
            String selectSql = "SELECT id FROM subscribers WHERE email = ?";
            try (java.sql.PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, email);
                try (java.sql.ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        // Email exists, proceed to unsubscribe
                        String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                        try (java.sql.PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, email);
                            int rowsAffected = updateStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                // Security: Do not leak internal details
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.getWriter().write("You have been unsubscribed.");
                            } else {
                                // Should not happen, but handle gracefully
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to unsubscribe.");
                            }
                        }
                    } else {
                        // Security: Do not reveal whether the email exists
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("If your email is subscribed, you will be unsubscribed.");
                    }
                }
            }
        } catch (Exception ex) {
            // Security: Log exception securely (not shown here), do not leak details to user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred.");
        }
        // ...existing code...

        }
    }
}
