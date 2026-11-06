import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UnsubscribeServlet
 *
 * Security decisions / Notes:
 *  - Uses JNDI DataSource (java:comp/env/jdbc/MyAppDS) for least-privileged DB access.
 *  - Validates email via a strict regex; rejects any malformed input.
 *  - Uses parameterized queries (PreparedStatement) to prevent SQL injection.
 *  - Does not reveal internal errors to the client; logs details server‐side only.
 *  - Sets no-store cache headers to prevent sensitive data caching.
 *  - Enforces UTF-8 output and text/plain content type.
 *  - State‐changing operation via GET is not recommended in REST; ideally should be POST with CSRF protection.
 */
public class O4_Mini_Scenario2_Copilot6_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // SLF4J logger for secure server-side logging
        private static final Logger logger = LoggerFactory.getLogger(UnsubscribeServlet.class);

        // Strict email validation per OWASP recommendations
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
        );

        // DataSource injected via JNDI (configured in web.xml / context.xml)
        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                InitialContext ctx = new InitialContext();
                // Lookup must match your JNDI resource name
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDS");
            } catch (NamingException ne) {
                logger.error("Failed to initialize DataSource", ne);
                throw new ServletException("Servlet initialization failed", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Prevent caching of sensitive responses
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }

            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                // Do not reveal which part failed (too much detail)
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Proceed to check and unsubscribe
            try (Connection conn = dataSource.getConnection()) {
                // Principle of least privilege: only SELECT and UPDATE on subscription table
                String selectSql = "SELECT 1 FROM subscriptions WHERE email = ? AND unsubscribed = FALSE";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            // Either not subscribed or already unsubscribed
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found or already unsubscribed.");
                            return;
                        }
                    }
                }

                String updateSql = "UPDATE subscriptions SET unsubscribed = TRUE, unsubscribed_at = CURRENT_TIMESTAMP WHERE email = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int updated = updateStmt.executeUpdate();
                    if (updated != 1) {
                        // Unexpected row count; roll back or alert if needed
                        logger.warn("Unexpected update count for email {}: {}", email, updated);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to unsubscribe at this time.");
                        return;
                    }
                }

                // Success
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println("You have been successfully unsubscribed.");

            } catch (SQLException sqle) {
                // Log full stack trace server-side; send generic message client-side
                logger.error("Database error during unsubscribe for email " + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}