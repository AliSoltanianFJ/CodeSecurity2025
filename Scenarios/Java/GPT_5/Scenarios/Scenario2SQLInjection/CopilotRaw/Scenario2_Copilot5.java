// ...existing code...
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
import java.util.regex.Pattern;

public class Scenario2_Copilot5 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Database configuration - override via environment variables if available
        private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/mydb");
        private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "dbuser");
        private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "dbpassword");

        // Simple email validation (reasonable for server-side sanity check)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Missing 'email' parameter.");
                return;
            }
            email = email.trim();

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid email format.");
                return;
            }

            // Load JDBC driver if needed (safe to ignore if driver auto-loads)
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                // Not fatal if driver auto-registered; log for diagnostics
                LOGGER.log(Level.FINE, "JDBC driver class not found on classpath (may be auto-registered).", e);
            }

            // Use try-with-resources to avoid resource leaks; use prepared statements to prevent SQL injection
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Check if email exists and subscription status
                String selectSql = "SELECT subscribed FROM users WHERE email = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().println("Email not found.");
                            return;
                        }
                        boolean subscribed = rs.getBoolean("subscribed");
                        if (!subscribed) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().println("Email is already unsubscribed.");
                            return;
                        }
                    }
                }

                // Perform unsubscribe (atomic update)
                String updateSql = "UPDATE users SET subscribed = 0 WHERE email = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int rows = updateStmt.executeUpdate();
                    if (rows == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().println("Unsubscribed successfully.");
                    } else if (rows > 1) {
                        // Unexpected: multiple rows affected
                        LOGGER.log(Level.WARNING, "Multiple rows updated when unsubscribing email: {0}", email);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("Unexpected database state: multiple records updated.");
                    } else {
                        // Zero rows updated although we checked existence earlier
                        LOGGER.log(Level.WARNING, "No rows updated when unsubscribing email (race?): {0}", email);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("Unable to unsubscribe. Please try again later.");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for email: " + email, e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Server error. Please try again later.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error while processing unsubscribe for email: " + email, e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Server error. Please try again later.");
            }
        }
    }
}
// ...existing code...
