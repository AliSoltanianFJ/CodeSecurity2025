// ...existing code...
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot7_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Conservative email pattern and maximum length
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,63}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        // DB connection settings read from environment variables (do not hard-code secrets)
        private String dbUrl;
        private String dbUser;
        private String dbPassword;

        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            // Read DB credentials from environment variables. Configure these in your container.
            dbUrl = System.getenv("UNSUB_DB_URL");       // e.g. "jdbc:postgresql://dbhost:5432/mydb"
            dbUser = System.getenv("UNSUB_DB_USER");
            dbPassword = System.getenv("UNSUB_DB_PASS");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOG.warning("Database credentials not fully provided via environment variables.");
                // Do not throw here; allow servlet to start but operations will fail with 500 and safe logs.
            }
        }

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Security headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Referrer-Policy", "no-referrer");
            resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';");
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            String rawEmail = req.getParameter("email");
            if (rawEmail == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Missing required parameter.");
                }
                return;
            }

            String email = rawEmail.trim();
            // Basic length check
            if (email.length() == 0 || email.length() > MAX_EMAIL_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Invalid input.");
                }
                return;
            }

            // Validate email format
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Invalid input.");
                }
                return;
            }

            // Use parameterized queries to prevent SQL injection. Do not reveal whether the email existed.
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOG.warning("DB credentials missing; cannot process unsubscribe for an email.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Service temporarily unavailable.");
                }
                return;
            }

            // Typical subscribers table: id (pk), email (unique), unsubscribed (boolean), unsubscribed_at (timestamp)
            final String selectSql = "SELECT id, unsubscribed FROM subscribers WHERE email = ? LIMIT 1";
            final String updateSql = "UPDATE subscribers SET unsubscribed = TRUE, unsubscribed_at = ? WHERE id = ?";

            // Keep response generic to avoid confirming existence of an email address
            boolean operationAttempted = false;

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                // Use a short transaction
                conn.setAutoCommit(false);
                try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                    sel.setString(1, email);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            long id = rs.getLong("id");
                            boolean already = rs.getBoolean("unsubscribed");
                            if (!already) {
                                try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                                    upd.setTimestamp(1, Timestamp.from(Instant.now()));
                                    upd.setLong(2, id);
                                    int rows = upd.executeUpdate();
                                    if (rows != 1) {
                                        // Unexpected result; rollback to be safe
                                        conn.rollback();
                                        LOG.log(Level.WARNING, "Unexpected update row count when unsubscribing id={0}: rows={1}", new Object[]{id, rows});
                                    } else {
                                        conn.commit();
                                    }
                                }
                            } else {
                                // Already unsubscribed, nothing to do
                                conn.commit();
                            }
                        } else {
                            // Email not found; commit a no-op transaction (or just roll back)
                            conn.commit();
                        }
                    }
                } catch (SQLException e) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        LOG.log(Level.SEVERE, "Rollback failed", ex);
                    }
                    LOG.log(Level.WARNING, "Database operation failed", e);
                } finally {
                    operationAttempted = true;
                }
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Unable to obtain DB connection", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Service temporarily unavailable.");
                }
                return;
            }

            // Generic success message do not confirm whether the email was present
            if (operationAttempted) {
                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("If the address was subscribed, it has been removed. You will receive no further messages.");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Service temporarily unavailable.");
                }
            }
        }
    }
}
// ...existing code...
