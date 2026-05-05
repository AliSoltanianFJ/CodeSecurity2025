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

public class Scenario2_Copilot4_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            String email = req.getParameter("email");
            try (PrintWriter out = resp.getWriter()) {
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

                // Read database config from environment variables (configure these in your environment)
                final String DB_URL = System.getenv("DB_URL");               // e.g. "jdbc:postgresql://localhost:5432/mydb"
                final String DB_USER = System.getenv("DB_USER");             // may be null
                final String DB_PASSWORD = System.getenv("DB_PASSWORD");     // may be null
                final String JDBC_DRIVER = System.getenv("JDBC_DRIVER");     // optional, e.g. "org.postgresql.Driver"

                if (DB_URL == null || DB_URL.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server configuration error.");
                    LOGGER.severe("DB_URL environment variable is not set.");
                    return;
                }

                if (JDBC_DRIVER != null && !JDBC_DRIVER.isEmpty()) {
                    try {
                        Class.forName(JDBC_DRIVER);
                    } catch (ClassNotFoundException e) {
                        // Not fatal in many environments (drivers autoload), but log for diagnostics
                        LOGGER.log(Level.WARNING, "JDBC driver class not found: " + JDBC_DRIVER, e);
                    }
                }

                Connection conn = null;
                boolean previousAutoCommit = true;
                try {
                    conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    previousAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);

                    // Check existence and subscription status
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
                            boolean subscribed = rs.getBoolean("subscribed");
                            if (!subscribed) {
                                conn.rollback();
                                resp.setStatus(HttpServletResponse.SC_OK);
                                out.println("Email already unsubscribed.");
                                return;
                            }
                        }
                    }

                    // Update to unsubscribe (idempotent: only affects rows still subscribed)
                    String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, email);
                        int updated = updateStmt.executeUpdate();
                        if (updated == 1) {
                            conn.commit();
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Unsubscribed successfully.");
                        } else if (updated == 0) {
                            conn.rollback();
                            resp.setStatus(HttpServletResponse.SC_CONFLICT);
                            out.println("Unable to unsubscribe (concurrent change). Please try again.");
                        } else {
                            // Unexpected: multiple rows affected
                            conn.rollback();
                            LOGGER.log(Level.WARNING, "Multiple rows updated when unsubscribing email: {0}", email);
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Server error. Please try again later.");
                        }
                    }
                } catch (SQLException e) {
                    try {
                        if (conn != null && !conn.isClosed()) {
                            conn.rollback();
                        }
                    } catch (SQLException ex) {
                        LOGGER.log(Level.FINE, "Rollback failed", ex);
                    }
                    LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for email: " + email, e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                } finally {
                    if (conn != null) {
                        try {
                            conn.setAutoCommit(previousAutoCommit);
                        } catch (SQLException ignore) { /* best effort */ }
                        try {
                            conn.close();
                        } catch (SQLException ignore) { /* best effort */ }
                    }
                }
            } catch (IOException e) {
                // rethrow to comply with method signature and let container handle higher-level logging
                throw e;
            }
        }
    }
}
// ...existing code...
