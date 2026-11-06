import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that handles unsubscribe requests.
 *
 * Security considerations:
 *  - Input is strictly validated as a well-formed email address (RFC-compliant via Commons-Validator).
 *  - Uses a JNDI DataSource (jdbc/MyAppDS) with least-privilege DB user.
 *  - All SQL uses PreparedStatement to prevent injection.
 *  - Error responses do not leak stack traces or sensitive info.
 *  - Sensitive operations require HTTPS (enforced at container or proxy).
 *  - Response headers disable caching and add basic security headers.
 *  - SLF4J for safe logging (no logging of PII).
 */
public class O4_Mini_Scenario2_Copilot5_FT_MP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UnsubscribeServlet.class);

    // Thread-safe once initialized
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            // Lookup JDBC DataSource via JNDI. Configure in container with a least-privilege user.
            InitialContext ctx = new InitialContext();
            // Adjust the JNDI name to match your environment
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDS");
        } catch (NamingException ne) {
            log.error("Unable to initialize DataSource for UnsubscribeServlet", ne);
            throw new ServletException("DataSource initialization failed", ne);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Enforce HTTPS (usually done at container/load-balancer). Here we check and reject if not secure.
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure channel required");
            return;
        }

        // Disable caching of unsubscribe responses
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        
        // Basic security headers (adjust as per your CSP/frame policy)
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");

        // Always respond in UTF-8
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        // 1) Read and validate input
        String email = req.getParameter("email");
        if (email == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
            return;
        }
        email = email.trim();
        EmailValidator validator = EmailValidator.getInstance(true, true);
        if (!validator.isValid(email)) {
            // Do not log the exact value to avoid PII in logs
            log.warn("Received malformed unsubscribe request");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
            return;
        }

        // 2) Perform unsubscribe in the database
        // We mark the user as unsubscribed; do not reveal whether the email existed.
        final String SQL_UNSUBSCRIBE =
            "UPDATE subscribers SET unsubscribed = TRUE, unsubscribed_at = CURRENT_TIMESTAMP " +
            "WHERE email = ? AND unsubscribed = FALSE";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UNSUBSCRIBE)) {
            // Principle of Least Privilege: the DB user should only have
            // rights to UPDATE its own table.
            ps.setString(1, email);
            int rowsAffected = ps.executeUpdate();
            // We do not differentiate in the response between 0 or 1 rows to prevent
            // user enumeration.
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("If this address was subscribed, it has now been unsubscribed.");
        } catch (SQLException sqle) {
            // Log full stack trace for ops, but send a generic error to client
            log.error("Database error during unsubscribe operation", sqle);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request");
        }
    }
}