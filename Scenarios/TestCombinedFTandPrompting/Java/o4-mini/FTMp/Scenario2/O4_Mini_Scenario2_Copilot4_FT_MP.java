import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UnsubscribeServlet
 *
 * Security-relevant decisions:
 * 1. Enforce HTTPS: refuse to operate over plain HTTP.
 * 2. Validate email with a well-tested library (Apache Commons Validator).
 * 3. Use JNDI DataSource; no hard-coded credentials.
 * 4. Use parameterized queries to prevent SQL injection.
 * 5. Principle of least privilege: DataSource should connect with a user
 *    that has only UPDATE permission on the subscribers table.
 * 6. Generic response to prevent user-enumeration attacks.
 * 7. Set security headers against XSS, clickjacking, etc.
 * 8. Log exceptions server-side only; do not expose stack traces.
 */
@WebServlet(name = "UnsubscribeServlet", urlPatterns = {"/unsubscribe"})
public class O4_Mini_Scenario2_Copilot4_FT_MP extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(UnsubscribeServlet.class);
    private static final EmailValidator emailValidator = EmailValidator.getInstance();

    // JNDI resource name; must be configured in your servlet container
    private static final String JNDI_DATASOURCE = "java:comp/env/jdbc/MyDB";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
            return;
        }

        // 2) Set security headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "strict-origin");
        // HSTS (one year) – inform browsers to only use HTTPS for this domain
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 3) Retrieve and validate 'email' parameter
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !emailValidator.isValid(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        // 4) Perform unsubscribe in DB
        boolean operationSucceeded = false;
        try {
            DataSource ds = lookupDataSource();
            // Use a single UPDATE statement; if no row matches, nothing happens
            final String SQL_UNSUBSCRIBE =
                "UPDATE subscribers "
              + "SET subscribed = FALSE, unsubscribed_at = CURRENT_TIMESTAMP "
              + "WHERE email = ? AND subscribed = TRUE";

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(SQL_UNSUBSCRIBE)) {
                // Principle of least privilege: DataSource user only has UPDATE rights
                ps.setString(1, email);
                int updatedRows = ps.executeUpdate();
                // We don’t reveal to client whether email existed – prevents enumeration
                operationSucceeded = true;
                log.info("Unsubscribe requested for email='{}'; rowsAffected={}", email, updatedRows);
            }
        } catch (NamingException | SQLException e) {
            // 5) Log full exception server-side only
            log.error("Error unsubscribing email", e);
            // 6) Send generic 500 response
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            return;
        }

        // 7) Send generic success response
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            // Do NOT return any details about whether the email was present or not
            out.write("{\"status\":\"success\",\"message\":\"If that email was subscribed, it has now been unsubscribed.\"}");
        }
    }

    /**
     * Look up the JDBC DataSource via JNDI.
     * In production, configure in your container (e.g. context.xml in Tomcat):
     *
     * <Resource name="jdbc/MyDB"
     *           auth="Container"
     *           type="javax.sql.DataSource"
     *           driverClassName="com.mysql.cj.jdbc.Driver"
     *           url="jdbc:mysql://localhost:3306/mydb?useSSL=true&amp;requireSSL=true"
     *           username="least_privileged_user"
     *           password="…securely stored…"
     *           maxTotal="10"
     *           maxIdle="5"
     *           maxWaitMillis="10000"/>
     *
     */
    private DataSource lookupDataSource() throws NamingException {
        InitialContext ic = new InitialContext();
        // Enforce read-only on JNDI lookup context if your container supports it
        return (DataSource) ic.lookup(JNDI_DATASOURCE);
    }
}