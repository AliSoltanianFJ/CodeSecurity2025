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

public class Scenario2_Copilot1_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        // ...existing code...

        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Missing or empty 'email' parameter.");
                }
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Invalid email format.");
                }
                return;
            }

            // Database configuration from environment (do not hard-code credentials)
            final String DB_URL = System.getenv("DB_URL");           // required
            final String DB_USER = System.getenv("DB_USER");         // optional depending on driver/config
            final String DB_PASSWORD = System.getenv("DB_PASSWORD"); // optional
            final String JDBC_DRIVER = System.getenv("JDBC_DRIVER"); // optional

            if (DB_URL == null || DB_URL.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Server configuration error.");
                }
                LOG.severe("DB_URL environment variable is not set.");
                return;
            }

            if (JDBC_DRIVER != null && !JDBC_DRIVER.trim().isEmpty()) {
                try {
                    Class.forName(JDBC_DRIVER);
                } catch (ClassNotFoundException e) {
                    LOG.log(Level.WARNING, "JDBC driver class not found: " + JDBC_DRIVER, e);
                    // Not fatal if driver auto-registers; continue
                }
            }

            try (PrintWriter out = resp.getWriter()) {
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                    // Use transaction to ensure consistent update
                    boolean previousAutoCommit = conn.getAutoCommit();
                    try {
                        conn.setAutoCommit(false);

                        // Attempt to set subscribed = FALSE only if currently subscribed
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
                                LOG.log(Level.WARNING, "Multiple rows updated when unsubscribing email");
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                out.println("Server error. Please try again later.");
                                return;
                            }
                        }

                        // No rows changed by the update; check whether the email exists
                        String existsSql = "SELECT subscribed FROM subscribers WHERE email = ?";
                        try (PreparedStatement existsStmt = conn.prepareStatement(existsSql)) {
                            existsStmt.setString(1, email);
                            try (ResultSet rs = existsStmt.executeQuery()) {
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
                                    out.println("Email is already unsubscribed.");
                                    return;
                                } else {
                                    // Race condition or unexpected state: try an update again as fallback
                                    String fallbackUpdate = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                                    try (PreparedStatement fallback = conn.prepareStatement(fallbackUpdate)) {
                                        fallback.setString(1, email);
                                        int r = fallback.executeUpdate();
                                        if (r == 1) {
                                            conn.commit();
                                            resp.setStatus(HttpServletResponse.SC_OK);
                                            out.println("Unsubscribed successfully.");
                                            return;
                                        } else {
                                            conn.rollback();
                                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                            out.println("Unable to unsubscribe. Please try again later.");
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        try { conn.rollback(); } catch (SQLException ignore) { /* ignore */ }
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("Server error. Please try again later.");
                    } finally {
                        try { conn.setAutoCommit(previousAutoCommit); } catch (SQLException ignore) { /* ignore */ }
                    }
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, "Unable to obtain database connection.", e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                }
            } catch (IOException e) {
                // getWriter() can throw IOException; rethrow to respect method contract
                throw e;
            }
        }
        // ...existing code...
    }
}
