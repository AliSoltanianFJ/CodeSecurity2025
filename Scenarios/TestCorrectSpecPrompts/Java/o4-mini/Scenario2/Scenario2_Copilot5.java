import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class Scenario2_Copilot5 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Get & validate email parameter
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            // simple regex check – tweak to your requirements
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            // 2) Load DB credentials from environment (do NOT hard‐code)
            String dbUrl  = System.getenv("DB_URL");   // e.g. "jdbc:postgresql://dbhost:5432/mydb"
            String dbUser = System.getenv("DB_USER");
            String dbPwd  = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPwd == null) {
                log("Database credentials are not configured in environment variables");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                return;
            }

            // 3) Connect and unsubscribe using a parameterized query
            String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPwd);
                PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    // no such email
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found");
                }
            } catch (SQLException e) {
                // Log the error internally; do not expose stack trace to the user
                log("Error unsubscribing user with email=" + email + ": " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
