import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot9 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        // very basic email regex for demonstration
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email address.");
                return;
            }

            // Read JDBC config from environment (or you could read from a protected properties file)
            String jdbcUrl      = System.getenv("DB_URL");
            String jdbcUser     = System.getenv("DB_USER");
            String jdbcPassword = System.getenv("DB_PASSWORD");
            if (jdbcUrl == null || jdbcUser == null || jdbcPassword == null) {
                LOGGER.severe("Database credentials are not set in environment variables.");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server configuration error.");
                return;
            }

            // Use try-with-resources to ensure all resources are closed
            try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)) {
                conn.setAutoCommit(false);

                // 1) Check if the email exists
                String checkSql = "SELECT 1 FROM subscribers WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            // no such subscriber
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("Email not found in our subscriber list.");
                            conn.rollback();
                            return;
                        }
                    }
                }

                // 2) Unsubscribe (e.g. set a flag)
                String updateSql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int rows = updateStmt.executeUpdate();
                    if (rows == 1) {
                        conn.commit();
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("You have been unsubscribed successfully.");
                    } else {
                        // this should not normally happen if check passed
                        conn.rollback();
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                       "Unable to unsubscribe at this time.");
                    }
                }

            } catch (SQLException e) {
                // Log full exception on the server; do not expose stack trace to the client
                LOGGER.log(Level.SEVERE, "Database error during unsubscribe", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An internal error occurred. Please try again later.");
            }
        }
    }
}