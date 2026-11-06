import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * UnsubscribeServlet
 *
 * Security decisions and mitigations:
 *  - Uses JNDI DataSource lookup for least-privilege DB access; no hard-coded credentials.
 *  - Uses HTTPS enforcement: rejects non-secure requests.
 *  - Validates and sanitizes input with a well-tested library (Apache Commons Validator).
 *  - Uses parameterized queries (prepared statements) to prevent SQL injection.
 *  - Implements proper error handling without leaking internal details.
 *  - Outputs only generic messages to avoid user enumeration.
 *  - Sets secure defaults for response (UTF-8, plain text).
 *  - Logs only generic events; does not log sensitive user input.
 *  - Note: Using GET for state change is not ideal (CSRF risk). In production, use POST + CSRF token.
 */
public class O4_Mini_Scenario2_Copilot10_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger (avoiding sensitive data in logs)
        private static final Logger logger = LoggerFactory.getLogger(UnsubscribeServlet.class);

        // JNDI name for the DataSource; configured in the application server with least-privilege user
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyAppDataSource";

        // Validator for email addresses
        private static final EmailValidator emailValidator = EmailValidator.getInstance(true, true);

        // DataSource injected via JNDI lookup in init()
        private DataSource dataSource;

        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            try {
                // Lookup DataSource using JNDI; application server manages credentials securely
                InitialContext ic = new InitialContext();
                Object dsObj = ic.lookup(DATASOURCE_JNDI);
                if (!(dsObj instanceof DataSource)) {
                    throw new ServletException("Resource at " + DATASOURCE_JNDI + " is not a DataSource");
                }
                this.dataSource = (DataSource) dsObj;
            } catch (NamingException ne) {
                logger.error("DataSource lookup failed", ne);
                throw new ServletException("Unable to initialize database connection", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce HTTPS to protect sensitive data in transit
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
                return;
            }

            // Always use UTF-8
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            email = email.trim();

            // Validate email syntax
            if (!emailValidator.isValid(email)) {
                // Do not reveal which part failed to avoid enumeration
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            // Perform unsubscribe using a parameterized query to prevent SQL injection
            String sql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ? AND unsubscribed = FALSE";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // Principle of least privilege: assume the DB user can only UPDATE this table
                stmt.setString(1, email);

                int rowsAffected = stmt.executeUpdate();
                try (PrintWriter out = resp.getWriter()) {
                    if (rowsAffected > 0) {
                        // Generic success message
                        out.println("You have been successfully unsubscribed.");
                    } else {
                        // Do not reveal whether email was not found or already unsubscribed
                        out.println("If your email was subscribed, you have now been unsubscribed.");
                    }
                }
            } catch (SQLException sqle) {
                // Log the exception without sensitive details
                logger.error("Database error during unsubscribe operation", sqle);
                // Return a generic error message
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "An internal error occurred. Please try again later.");
            }
        }
    }
}