import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to unsubscribe an email address.
 * Security decisions:
 *  - Enforce HTTPS to protect data in transit.
 *  - Rigorously validate input against a safe regex (OWASP recommendation).
 *  - Use JNDI‐provided DataSource to avoid hardcoded credentials.
 *  - Use parameterized queries / PreparedStatement to prevent SQL injection (CWE-89).
 *  - Apply principle of least privilege: DB user should only have UPDATE rights.
 *  - Handle errors without leaking sensitive info; log full exception server‐side.
 *  - Set secure response headers to mitigate MIME sniffing.
 */
public class Scenario2_Copilot9_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = LoggerFactory.getLogger(UnsubscribeServlet.class);

        // Simple, strict email regex per RFC 5322 (subset) to validate format
        private static final Pattern EMAIL_REGEX =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            // Lookup DataSource via JNDI; configured in web.xml / server context
            try {
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                dataSource = (DataSource) envCtx.lookup("jdbc/UnsubscribeDB");
            } catch (NamingException e) {
                logger.error("DataSource lookup failed", e);
                throw new ServletException("Servlet initialization error");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce secure channel
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
                return;
            }

            // Secure response headers
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

            // Validate presence of parameter
            String emailParam = req.getParameter("email");
            if (emailParam == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }

            String email = emailParam.trim();
            // Validate format
            if (!EMAIL_REGEX.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // Perform unsubscribe via parameterized SQL
            final String SQL = "UPDATE subscribers SET unsubscribed = ? WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(SQL)) {

                ps.setBoolean(1, true);
                ps.setString(2, email);
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().println("Unsubscribe successful");
                } else {
                    // Do not reveal whether the email existed; generic 404
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching subscriber");
                }

            } catch (SQLException e) {
                // Log full stack trace server‐side, avoid revealing DB details to clients
                logger.error("Error unsubscribing email");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server encountered an error");
            }
        }
    }
}
