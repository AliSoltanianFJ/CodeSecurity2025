import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class O4_Mini_Scenario2_Copilot2_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for audit and error tracking; does not expose sensitive data
        private static final Logger logger = LoggerFactory.getLogger(UnsubscribeServlet.class);

        // Simple but strict email validation pattern (RFC-compatible patterns are more complex;
        // in production consider a library or more complete regex)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );

        // DataSource should be configured in the application server / container with least-privilege credentials.
        @Resource(lookup = "java:comp/env/jdbc/MyDB")
        private DataSource dataSource;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce HTTPS transport
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Secure connection required.");
                return;
            }

            // Security headers
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Referrer-Policy", "no-referrer");
            resp.setHeader("Cache-Control", "no-store");

            // Retrieve and sanitize input
            String emailParam = req.getParameter("email");
            if (emailParam == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            String email = emailParam.trim();

            // Validate format
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Prepare parameterized SQL to avoid injection; only updates if not already unsubscribed
            final String sql =
                "UPDATE subscribers " +
                "SET unsubscribed = TRUE, unsubscribed_at = CURRENT_TIMESTAMP " +
                "WHERE email = ? AND unsubscribed = FALSE";

            try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
            ) {
                // Principle of least privilege: DataSource user should only have UPDATE permission on this table
                ps.setString(1, email);
                int rowsAffected = ps.executeUpdate();

                resp.setContentType("application/json; charset=UTF-8");
                if (rowsAffected > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"status\":\"unsubscribed\"}");
                } else {
                    // Do not reveal whether email was never present or already unsubscribed
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"status\":\"not_found_or_already_unsubscribed\"}");
                }

            } catch (SQLException e) {
                // Log full stacktrace server-side; do not reveal internals to client
                logger.error("Database error while unsubscribing email={}", email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
