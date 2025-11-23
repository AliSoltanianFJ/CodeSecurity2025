// ...existing code...
import javax.servlet.annotation.WebServlet;
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
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot6_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple, conservative email pattern (keeps validation strict but practical)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9._%+\\-']+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );

        // Limit lengths to avoid resource exhaustion / buffer-like issues
        private static final int MAX_EMAIL_LENGTH = 254;

        // Generic response to prevent account enumeration
        private static final String GENERIC_RESPONSE = "If this address is subscribed, it has been unsubscribed. " +
                "If you did not request this, please contact support.";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

            // Canonicalize and trim input
            String email = rawEmail.trim();

            // Basic validation: length and pattern
            if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Invalid email parameter.");
                }
                return;
            }

            // Obtain DB connection info from environment do not hardcode credentials in source
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (Objects.isNull(dbUrl)) {
                // Misconfiguration don't reveal details to client
                LOGGER.log(Level.SEVERE, "Database URL not configured (DB_URL env var missing) at {0}", Instant.now());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Service unavailable.");
                }
                return;
            }

            // Perform DB operations safely using prepared statements and try-with-resources
            // Table and column names assumed: users(email VARCHAR UNIQUE, subscribed BOOLEAN)
            // The implementation will not reveal whether the email existed to avoid enumeration attacks.
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // Use a short transaction for consistency
                conn.setAutoCommit(false);

                String selectSql = "SELECT subscribed FROM users WHERE email = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);

                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            boolean subscribed = rs.getBoolean("subscribed");
                            if (subscribed) {
                                String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setString(1, email);
                                    int updated = updateStmt.executeUpdate();
                                    if (updated == 0) {
                                        // Unexpected: row existed but update affected 0 rows. Log and continue.
                                        LOGGER.log(Level.WARNING, "Expected to update subscription for {0} but updateCount=0", email);
                                    }
                                }
                            }
                        }
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                // Log server-side details for investigation, but don't leak internals to the client
                LOGGER.log(Level.SEVERE, "Database operation failed: {0}", ex.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Service unavailable.");
                }
                return;
            }

            // Always return the same generic message to prevent user-account enumeration
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter w = resp.getWriter()) {
                w.println(GENERIC_RESPONSE);
            }
        }
    }
}
// ...existing code...
