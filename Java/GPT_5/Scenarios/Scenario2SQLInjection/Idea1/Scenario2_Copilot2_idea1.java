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

public class Scenario2_Copilot2_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            String email = req.getParameter("email");
            try (PrintWriter out = resp.getWriter()) {
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

                String dbUrl = System.getenv("DB_URL");
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");
                String jdbcDriver = System.getenv("JDBC_DRIVER"); // optional

                if (dbUrl == null || dbUrl.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server configuration error.");
                    LOGGER.severe("DB_URL environment variable is not set.");
                    return;
                }

                if (jdbcDriver != null && !jdbcDriver.isEmpty()) {
                    try {
                        Class.forName(jdbcDriver);
                    } catch (ClassNotFoundException e) {
                        // Driver may be auto-registered; log for diagnostics but continue
                        LOGGER.log(Level.FINE, "JDBC driver class not found on classpath: " + jdbcDriver, e);
                    }
                }

                Connection conn = null;
                try {
                    conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                    conn.setAutoCommit(false);

                    // Check existence and subscription status
                    String selectSql = "SELECT subscribed FROM users WHERE email = ?";
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        ps.setString(1, email);
                        try (ResultSet rs = ps.executeQuery()) {
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
                            }
                        }
                    }

                    // Perform unsubscribe
                    String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, email);
                        int updated = ps.executeUpdate();
                        if (updated == 1) {
                            conn.commit();
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Unsubscribed successfully.");
                            return;
                        } else {
                            conn.rollback();
                            LOGGER.log(Level.WARNING, "Unexpected update count when unsubscribing {0}: {1}", new Object[]{email, updated});
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            out.println("Unable to unsubscribe. Please try again later.");
                            return;
                        }
                    }
                } catch (SQLException e) {
                    try {
                        if (conn != null && !conn.isClosed()) conn.rollback();
                    } catch (SQLException ignore) {
                    }
                    LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe request for email: " + email, e);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Server error. Please try again later.");
                    return;
                } finally {
                    if (conn != null) {
                        try {
                            conn.setAutoCommit(true);
                        } catch (SQLException ignore) {
                        }
                        try {
                            conn.close();
                        } catch (SQLException ignore) {
                        }
                    }
                }
            } catch (IOException e) {
                // rethrow as declared by servlet API method signature
                throw e;
            }
        }
    }
}
