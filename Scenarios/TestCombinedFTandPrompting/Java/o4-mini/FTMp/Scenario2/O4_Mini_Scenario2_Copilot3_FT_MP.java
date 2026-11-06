package com.example.unsubscribe;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scenario2 contains the UnsubscribeServlet which securely processes
 * unsubscribe requests via a GET parameter "email".
 *
 * Security decisions:
 *  - All inputs are validated against a strict email regex.
 *  - Database access uses a JNDI DataSource with least-privilege credentials.
 *  - SQL UPDATE is performed with a parameterized PreparedStatement to prevent SQL injection.
 *  - HTTPS is enforced; HTTP requests are redirected to HTTPS.
 *  - Response headers set to mitigate XSS/Clickjacking.
 *  - Generic responses avoid revealing subscription status.
 *  - Errors are logged internally; clients only see generic messages.
 *  - No sensitive details (stack traces, SQL errors) are sent to the client.
 */
public class O4_Mini_Scenario2_Copilot3_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        // Strict email pattern (simplified RFC5322-ish)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        );

        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // JNDI DataSource for database connectivity (no hardcoded credentials)
        private DataSource dataSource;

        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            try {
                // Lookup the DataSource via JNDI. Ensure the resource is configured in context.xml/web.xml
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                // "jdbc/UnsubscribeDS" should be bound to a secure, least-privilege DB user
                dataSource = (DataSource) envCtx.lookup("jdbc/UnsubscribeDS");
            } catch (NamingException ne) {
                LOGGER.log(Level.SEVERE, "Failed to initialize DataSource", ne);
                throw new ServletException("Internal server error", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                String query = req.getQueryString();
                String redirectUrl = "https://" + req.getServerName()
                        + req.getRequestURI()
                        + (query != null ? "?" + query : "");
                resp.setStatus(HttpServletResponse.SC_FOUND);
                resp.setHeader("Location", redirectUrl);
                return;
            }

            // Security response headers
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Content-Security-Policy", "default-src 'none';");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            email = email.trim();
            if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                // Do not reveal pattern details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // Perform the unsubscribe operation
            String updateSql = "UPDATE subscribers SET unsubscribed = ? WHERE email = ?";
            try (
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(updateSql)
            ) {
                // Principle of least privilege: the DB user should only have UPDATE rights
                stmt.setBoolean(1, true);
                stmt.setString(2, email);
                int rowsAffected = stmt.executeUpdate();
                // Always return the same generic message to avoid user enumeration
                try (PrintWriter out = resp.getWriter()) {
                    out.println("If that address was subscribed, it has now been unsubscribed.");
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                LOGGER.log(Level.INFO,
                        "Unsubscribe attempt for email={0}, rowsAffected={1}",
                        new Object[] { email, rowsAffected }
                );
            } catch (SQLException sqle) {
                // Log internally, do not leak SQL errors to client
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }

        @Override
        public void destroy() {
            // No resources to clean up explicitly; container will handle DataSource
            super.destroy();
        }
    }
}
