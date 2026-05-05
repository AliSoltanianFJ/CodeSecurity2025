// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot4_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        /*
         * Checks the "email" GET parameter. If present and valid, attempt to
         * mark the subscriber as unsubscribed in the database. Uses a
         * prepared statement to avoid SQL injection and reads DB credentials
         * from environment variables to avoid hardcoding secrets.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            email = email.trim();

            // Basic email format check (keeps things simple; not full RFC compliance)
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Read DB connection info from environment variables (set these in your container/app server)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUrl.isEmpty() || dbUser == null || dbPassword == null) {
                LOG.log(Level.SEVERE, "Database connection settings are not configured in environment variables.");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            // SQL: set subscribed flag to false (use appropriate column name/type for your schema)
            final String updateSql = "UPDATE subscribers SET subscribed = ? WHERE email = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {

                // Use parameter binding to avoid SQL injection
                ps.setBoolean(1, false);
                ps.setString(2, email);

                int affected = ps.executeUpdate();

                try (PrintWriter out = resp.getWriter()) {
                    if (affected > 0) {
                        out.println("Unsubscribed.");
                    } else {
                        out.println("Email not found.");
                    }
                }
            } catch (SQLException e) {
                // Log internal details, but return a generic message to the client
                LOG.log(Level.SEVERE, "Database error while processing unsubscribe request", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
// ...existing code...
