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

public class Scenario2_Copilot6_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic server-side email sanity check (not exhaustive)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
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

            // Read DB configuration from environment variables (recommended)
            final String DB_URL = System.getenv("DB_URL"); // e.g. "jdbc:postgresql://localhost:5432/mydb"
            final String DB_USER = System.getenv("DB_USER"); // may be null
            final String DB_PASSWORD = System.getenv("DB_PASSWORD"); // may be null
            final String JDBC_DRIVER = System.getenv("JDBC_DRIVER"); // optional, e.g. "org.postgresql.Driver"

            if (DB_URL == null || DB_URL.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Server configuration error.");
                LOGGER.severe("DB_URL not configured");
                return;
            }

            try {
                if (JDBC_DRIVER != null && !JDBC_DRIVER.isEmpty()) {
                    try {
                        Class.forName(JDBC_DRIVER);
                    } catch (ClassNotFoundException e) {
                        // Not fatal if driver auto-registers; log for diagnostics
                        LOGGER.log(Level.FINE, "JDBC driver class not found on classpath (may be auto-registered).", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unexpected error loading JDBC driver", e);
                // continue; driver load failures are non-fatal if driver auto-registers
            }

            Connection conn = null;
            boolean prevAutoCommit = true;
            try {
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                prevAutoCommit = conn.getAutoCommit();
                conn.setAutoCommit(false);

                // Check existence and subscription status
                String selectSql = "SELECT subscribed FROM users WHERE email = ?";
                boolean found;
                boolean subscribed;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            out.println("Email not found.");
                            return;
                        }
                        found = true;
                        subscribed = rs.getBoolean(1);
                    }
                }

                if (!found) {
                    // defensive, should not be reached due to earlier return
                    conn.rollback();
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.println("Email not found.");
                    return;
                }

                if (!subscribed) {
                    conn.rollback();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    out.println("Email is already unsubscribed.");
                    return;
                }

                // Attempt to atomically mark as unsubscribed
                String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int affected = updateStmt.executeUpdate();
                    if (affected == 1) {
                        conn.commit();
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("Unsubscribed successfully.");
                        return;
                    } else if (affected == 0) {
                        conn.rollback();
                        LOGGER.log(Level.WARNING, "No rows updated when unsubscribing email: {0}", email);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Unable to unsubscribe. Please try again later.");
                        return;
                    } else {
                        // unexpected: multiple rows updated
                        conn.rollback();
                        LOGGER.log(Level.WARNING, "Multiple rows updated when unsubscribing email: {0}", email);
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Server error. Please contact support.");
                        return;
                    }
                }

            } catch (SQLException e) {
                // Attempt rollback on DB error
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ignore) { /* best-effort rollback */ }
                }
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for email: " + email, e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Server error. Please try again later.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error while processing unsubscribe for email: " + email, e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Server error. Please try again later.");
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(prevAutoCommit);
                    } catch (SQLException ignore) { /* best-effort */ }
                    try {
                        conn.close();
                    } catch (SQLException ignore) { /* best-effort */ }
                }
            }
        }
    }
}
