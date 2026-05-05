// ...existing code...
package scenarios;

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
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot10_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple, conservative email validation for server-side sanity checks.
        // Not exhaustive but prevents obvious invalid values and very long input.
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]{1,255}\\.[^@\\s]{2,64}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            try (PrintWriter out = resp.getWriter()) {
                if (email == null || (email = email.trim()).isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Missing or empty 'email' parameter.");
                    return;
                }

                if (email.length() > 320 || !EMAIL_PATTERN.matcher(email).matches()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Invalid email format.");
                    return;
                }

                // Read DB configuration from environment variables (recommended for production)
                final String DB_URL = System.getenv("DB_URL");
                final String DB_USER = System.getenv("DB_USER");
                final String DB_PASSWORD = System.getenv("DB_PASSWORD");
                final String JDBC_DRIVER = System.getenv("JDBC_DRIVER"); // optional

                if (DB_URL == null || DB_URL.isEmpty()) {
                    LOGGER.log(Level.SEVERE, "Database URL not configured (DB_URL env missing).");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server configuration error.");
                    return;
                }

                // Optionally load JDBC driver class if provided
                if (JDBC_DRIVER != null && !JDBC_DRIVER.isEmpty()) {
                    try {
                        Class.forName(JDBC_DRIVER);
                    } catch (ClassNotFoundException e) {
                        LOGGER.log(Level.WARNING, "JDBC driver class not found: {0}", JDBC_DRIVER);
                        // proceed: many drivers auto-register
                    }
                }

                // Use prepared statements and transactions to avoid SQL injection and ensure consistency.
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    boolean previousAutoCommit = conn.getAutoCommit();
                    try {
                        conn.setAutoCommit(false);

                        // Check existence and subscription status. Adjust table/column names to match schema.
                        String selectSql = "SELECT subscribed FROM subscribers WHERE email = ?";
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
                                } catch (SQLException ignore) {
                                    // If the column doesn't exist or isn't boolean, treat as subscribed (safe fallback).
                                    subscribed = true;
                                }
                                if (!subscribed) {
                                    conn.rollback();
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    out.println("Email is already unsubscribed.");
                                    return;
                                }
                            }
                        }

                        // Perform update to mark unsubscribed. This is atomic within the transaction.
                        String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, email);
                            int updated = updateStmt.executeUpdate();
                            if (updated == 1) {
                                conn.commit();
                                resp.setStatus(HttpServletResponse.SC_OK);
                                out.println("Unsubscribed successfully.");
                                return;
                            } else if (updated > 1) {
                                // Unexpected: multiple rows affected
                                conn.rollback();
                                LOGGER.log(Level.SEVERE, "Multiple rows updated when unsubscribing email: {0}", email);
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                out.println("Server error.");
                                return;
                            } else {
                                // Race or schema difference: try delete as fallback (non-destructive fallback can be removed)
                                conn.rollback();
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                out.println("Unable to unsubscribe. Please try again later.");
                                return;
                            }
                        }
                    } catch (SQLException e) {
                        try { conn.rollback(); } catch (SQLException ignore) { /* best-effort */ }
                        LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for email.", e);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Server error.");
                    } finally {
                        try { conn.setAutoCommit(previousAutoCommit); } catch (SQLException ignore) { /* best-effort */ }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Unable to connect to database.", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error.");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Unexpected error while processing unsubscribe.", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error.");
                }
            } catch (IOException e) {
                // Writer acquisition failed or stream write error; rethrow as declared by signature
                throw e;
            }
        }
    }
}
// ...existing code...
