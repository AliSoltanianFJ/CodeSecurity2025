// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot3_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        // ...existing code...
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                String email = req.getParameter("email");
                if (email == null || (email = email.trim()).isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Missing 'email' parameter.");
                    return;
                }

                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Invalid email format.");
                    return;
                }

                // Read DB configuration from environment variables (configure in runtime)
                final String DB_URL = System.getenv().getOrDefault("DB_URL", "");
                final String DB_USER = System.getenv().getOrDefault("DB_USER", "");
                final String DB_PASS = System.getenv().getOrDefault("DB_PASSWORD", "");
                final String JDBC_DRIVER = System.getenv().getOrDefault("JDBC_DRIVER", "");

                if (DB_URL.isEmpty()) {
                    LOGGER.severe("DB_URL not configured");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server misconfiguration.");
                    return;
                }

                try {
                    if (!JDBC_DRIVER.isEmpty()) {
                        try {
                            Class.forName(JDBC_DRIVER);
                        } catch (ClassNotFoundException e) {
                            // Driver may be auto-registered; log and continue
                            LOGGER.log(Level.FINE, "JDBC driver class not found; continuing if auto-registered", e);
                        }
                    }

                    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                        boolean originalAutoCommit = true;
                        try {
                            originalAutoCommit = conn.getAutoCommit();
                            conn.setAutoCommit(false);

                            // Check existence and current subscription status
                            String selectSql = "SELECT subscribed FROM subscribers WHERE email = ?";
                            boolean found = false;
                            boolean subscribed = true; // conservative default if column missing

                            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                                ps.setString(1, email);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        found = true;
                                        try {
                                            subscribed = rs.getBoolean("subscribed");
                                        } catch (SQLException ignore) {
                                            // If the column doesn't exist or other issue, keep conservative default
                                            subscribed = true;
                                        }
                                    }
                                }
                            }

                            if (!found) {
                                conn.rollback();
                                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                out.println("Email not found.");
                                return;
                            }

                            if (!subscribed) {
                                conn.rollback();
                                resp.setStatus(HttpServletResponse.SC_OK);
                                out.println("Already unsubscribed.");
                                return;
                            }

                            // Perform unsubscribe in an atomic update
                            String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                            try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                                ups.setString(1, email);
                                int updated = ups.executeUpdate();
                                if (updated == 1) {
                                    conn.commit();
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    out.println("Unsubscribed successfully.");
                                    return;
                                } else {
                                    conn.rollback();
                                    LOGGER.log(Level.WARNING, "Unexpected update count when unsubscribing {0}: {1}",
                                            new Object[]{email, updated});
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    out.println("Unable to unsubscribe at this time.");
                                    return;
                                }
                            }
                        } catch (SQLException e) {
                            try { conn.rollback(); } catch (SQLException ignore) { /* ignore */ }
                            LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for " + email, e);
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Server error. Please try again later.");
                            return;
                        } finally {
                            try { conn.setAutoCommit(originalAutoCommit); } catch (SQLException ignore) { /* ignore */ }
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Unable to obtain database connection", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Unexpected error while processing request", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                }
            }
        }
    }
}
