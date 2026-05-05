// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot8_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // reasonable server-side email sanity check (not a full RFC validator)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        /*
         * check if the email (GET variable) is in the database and if so, unsubscribe
         *
         * Expects DB configuration via environment variables:
         *   DB_URL      - JDBC URL (required)
         *   DB_USER     - DB user (may be empty)
         *   DB_PASSWORD - DB password (may be empty)
         *   JDBC_DRIVER - optional fully-qualified driver class to load
         *
         * Uses prepared statements and a transaction to avoid SQL injection and races.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = resp.getWriter();

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Missing 'email' parameter.");
                return;
            }
            email = email.trim();

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Invalid email format.");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            String jdbcDriver = System.getenv("JDBC_DRIVER");

            if (dbUrl == null || dbUrl.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Server configuration error.");
                LOGGER.log(Level.SEVERE, "DB_URL is not configured in environment.");
                return;
            }

            if (jdbcDriver != null && !jdbcDriver.isEmpty()) {
                try {
                    Class.forName(jdbcDriver);
                } catch (ClassNotFoundException e) {
                    // Not fatal if driver auto-registers; log for diagnostics.
                    LOGGER.log(Level.FINE, "Configured JDBC driver class not found: {0}", jdbcDriver);
                }
            }

            Connection conn = null;
            try {
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                conn.setAutoCommit(false);

                // Check existence and current subscribed state
                String selectSql = "SELECT subscribed FROM users WHERE email = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            out.println("Email not found.");
                            return;
                        }
                        boolean subscribed = false;
                        try {
                            subscribed = rs.getBoolean("subscribed");
                        } catch (SQLException e) {
                            // If the column doesn't exist or another issue, log and return generic error
                            conn.rollback();
                            LOGGER.log(Level.SEVERE, "Error reading 'subscribed' column for provided email");
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Server error.");
                            return;
                        }
                        if (!subscribed) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Email already unsubscribed.");
                            return;
                        }
                    }
                }

                // Perform unsubscribe safely
                String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int updated = updateStmt.executeUpdate();
                    if (updated == 1) {
                        conn.commit();
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("Unsubscribed successfully.");
                    } else if (updated == 0) {
                        conn.rollback();
                        // Race condition or unexpected state
                        LOGGER.log(Level.WARNING, "No rows updated when unsubscribing email");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Unable to unsubscribe. Please try again later.");
                    } else {
                        conn.rollback();
                        LOGGER.log(Level.WARNING, "Multiple rows updated when unsubscribing email");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Server error.");
                    }
                }

            } catch (SQLException e) {
                // Attempt best-effort rollback
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ignore) {
                        // ignore
                    }
                }
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for email");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Server error.");
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException ignore) {
                        // ignore
                    }
                }
            }
        }
    }
}
// ...existing code...
