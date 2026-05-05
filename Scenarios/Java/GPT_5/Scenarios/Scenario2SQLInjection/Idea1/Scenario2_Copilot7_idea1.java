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

public class Scenario2_Copilot7_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Reasonable server-side sanity check for an email address
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                String email = req.getParameter("email");
                if (email == null || (email = email.trim()).isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Missing or empty 'email' parameter.");
                    return;
                }

                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Invalid email format.");
                    return;
                }

                // Read DB configuration from environment variables (recommended for production)
                final String dbUrl = System.getenv("DB_URL"); // e.g. "jdbc:postgresql://localhost:5432/mydb"
                final String dbUser = System.getenv("DB_USER");
                final String dbPassword = System.getenv("DB_PASSWORD");
                final String jdbcDriver = System.getenv("JDBC_DRIVER"); // optional, e.g. "org.postgresql.Driver"

                if (dbUrl == null || dbUrl.isEmpty()) {
                    LOGGER.log(Level.SEVERE, "Database URL not configured (DB_URL).");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server misconfiguration.");
                    return;
                }

                // Try to load provided JDBC driver if specified (optional)
                if (jdbcDriver != null && !jdbcDriver.isEmpty()) {
                    try {
                        Class.forName(jdbcDriver);
                    } catch (ClassNotFoundException e) {
                        // Not fatal if driver auto-registers; log for diagnostics
                        LOGGER.log(Level.FINE, "JDBC driver class not found on classpath (may be auto-registered): " + jdbcDriver, e);
                    }
                }

                // Use try-with-resources to ensure connection and statements are closed
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                    // Use a transaction to avoid race conditions
                    boolean originalAutoCommit = conn.getAutoCommit();
                    try {
                        conn.setAutoCommit(false);

                        boolean found = false;
                        boolean hasSubscribedColumn = true;
                        boolean subscribed = false;

                        // First attempt: try to read a 'subscribed' boolean column if present
                        final String selectSubscribedSql = "SELECT subscribed FROM subscribers WHERE email = ?";
                        try (PreparedStatement ps = conn.prepareStatement(selectSubscribedSql)) {
                            ps.setString(1, email);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    found = true;
                                    try {
                                        subscribed = rs.getBoolean("subscribed");
                                    } catch (SQLException colEx) {
                                        // Column may not exist or be named differently; fall back below
                                        hasSubscribedColumn = false;
                                        LOGGER.log(Level.FINE, "No 'subscribed' column or cannot read it; will fall back to existence-only flow.", colEx);
                                    }
                                }
                            }
                        } catch (SQLException readEx) {
                            // If the select fails (for example column missing or different schema), fall back to existence-only check
                            LOGGER.log(Level.FINE, "Reading 'subscribed' column failed; falling back to existence check.", readEx);
                            hasSubscribedColumn = false;
                        }

                        // If we couldn't determine existence from the first select, or subscribed column not available, check existence explicitly
                        if (!found && !hasSubscribedColumn) {
                            final String selectExistSql = "SELECT 1 FROM subscribers WHERE email = ?";
                            try (PreparedStatement ps2 = conn.prepareStatement(selectExistSql)) {
                                ps2.setString(1, email);
                                try (ResultSet rs2 = ps2.executeQuery()) {
                                    if (rs2.next()) {
                                        found = true;
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

                        // If we know the subscribed flag and it's already false
                        if (hasSubscribedColumn && !subscribed) {
                            // nothing to change
                            conn.commit();
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Email is already unsubscribed.");
                            return;
                        }

                        // If schema has a subscribed flag, prefer an atomic update; otherwise delete the row
                        if (hasSubscribedColumn) {
                            final String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, email);
                                int affected = updateStmt.executeUpdate();
                                if (affected == 1) {
                                    conn.commit();
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    out.println("Unsubscribed successfully.");
                                    return;
                                } else if (affected == 0) {
                                    // Could be a race or unexpected state
                                    conn.rollback();
                                    LOGGER.log(Level.WARNING, "Update affected 0 rows when trying to unsubscribe");
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    out.println("Unable to unsubscribe. Please try again later.");
                                    return;
                                } else {
                                    // Unexpected multiple rows affected
                                    conn.rollback();
                                    LOGGER.log(Level.WARNING, "Update affected multiple rows when unsubscribing");
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    out.println("Server error. Please try again later.");
                                    return;
                                }
                            }
                        } else {
                            // Schema does not have a subscribed flag; remove the subscription row
                            final String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                                delStmt.setString(1, email);
                                int deleted = delStmt.executeUpdate();
                                if (deleted >= 1) {
                                    conn.commit();
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    out.println("Unsubscribed successfully.");
                                    return;
                                } else {
                                    conn.rollback();
                                    LOGGER.log(Level.WARNING, "Delete affected 0 rows when trying to unsubscribe user");
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    out.println("Unable to unsubscribe. Please try again later.");
                                    return;
                                }
                            }
                        }
                    } catch (SQLException ex) {
                        // Attempt a rollback; log but don't expose details to client
                        try {
                            conn.rollback();
                        } catch (SQLException rbEx) {
                            LOGGER.log(Level.FINE, "Rollback failed", rbEx);
                        }
                        LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for user email");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Server error. Please try again later.");
                    } finally {
                        // Best-effort: restore original auto-commit
                        try {
                            conn.setAutoCommit(originalAutoCommit);
                        } catch (SQLException ignore) {
                            LOGGER.log(Level.FINE, "Failed to restore auto-commit", ignore);
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Unable to obtain database connection.", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                }
            } catch (IOException ioe) {
                // Let the container handle IOExceptions after logging; method already declares IOException
                LOGGER.log(Level.SEVERE, "I/O error writing response", ioe);
                throw ioe;
            } catch (Exception unexpected) {
                // Catch-all to avoid leaking stack traces to clients
                LOGGER.log(Level.SEVERE, "Unexpected error while processing unsubscribe request", unexpected);
                try {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().println("Server error. Please try again later.");
                } catch (IOException ignore) {
                    LOGGER.log(Level.FINE, "Failed to write error response", ignore);
                }
            }
        }
    }
}
// ...existing code...
