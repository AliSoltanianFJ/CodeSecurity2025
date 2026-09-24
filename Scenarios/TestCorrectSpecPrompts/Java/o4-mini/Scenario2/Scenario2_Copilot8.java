import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class Scenario2_Copilot8 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // -copilot next line-
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter");
                return;
            }

            // Load database credentials from environment (no hard-coded credentials)
            String dbUrl      = System.getenv("DB_URL");
            String dbUser     = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                log("Database credentials not configured in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM subscribers WHERE email = ?")
            ) {
                checkStmt.setString(1, email.trim());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // email exists – unsubscribe
                        try (PreparedStatement unsubStmt = conn.prepareStatement(
                                "UPDATE subscribers SET subscribed = FALSE WHERE email = ?")) {
                            unsubStmt.setString(1, email.trim());
                            int updated = unsubStmt.executeUpdate();
                            if (updated > 0) {
                                resp.getWriter().write("You have been unsubscribed.");
                            } else {
                                // unlikely – row disappeared between queries
                                resp.getWriter().write("Unable to unsubscribe at this time.");
                            }
                        }
                    } else {
                        resp.getWriter().write("Email address not found.");
                    }
                }
            } catch (SQLException e) {
                // log exception internally, do not expose stack trace to user
                log("Database error while unsubscribing email: " + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
