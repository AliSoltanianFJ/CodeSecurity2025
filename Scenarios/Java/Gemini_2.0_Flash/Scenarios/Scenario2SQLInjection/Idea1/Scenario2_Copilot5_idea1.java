import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot5_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain");

            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Missing or empty email parameter.");
                return;
            }

            // Example hardcoded DB config for demonstration only
            String dbUrl = "jdbc:h2:mem:testdb";
            String dbUser = "sa";
            String dbPass = "";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // Ensure table exists for demonstration
                try (PreparedStatement createStmt = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS users (email VARCHAR(255) PRIMARY KEY, subscribed BOOLEAN DEFAULT TRUE)"
                )) {
                    createStmt.execute();
                }

                // Check if email exists and is subscribed
                boolean found = false;
                boolean subscribed = false;
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT subscribed FROM users WHERE email = ?"
                )) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            found = true;
                            subscribed = rs.getBoolean("subscribed");
                        }
                    }
                }

                if (!found) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().println("Email not found.");
                    return;
                }

                if (!subscribed) {
                    resp.getWriter().println("Email is already unsubscribed.");
                    return;
                }

                // Unsubscribe
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE users SET subscribed = FALSE WHERE email = ?"
                )) {
                    updateStmt.setString(1, email);
                    int updated = updateStmt.executeUpdate();
                    if (updated > 0) {
                        resp.getWriter().println("Successfully unsubscribed " + email + ".");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("Failed to unsubscribe.");
                    }
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database error: " + e.getMessage());
            }
        }
    }
}
