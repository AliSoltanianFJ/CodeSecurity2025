import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UnsubscribeServlet securely handles unsubscribe requests.
 *
 * Security decisions documented inline:
 *  - Uses GET only to provide a simple unsubscribe link workflow.
 *  - Validates and sanitizes the 'email' parameter strictly via a safe regex.
 *  - Uses JNDI DataSource lookup; no credentials are hardcoded.
 *  - Uses PreparedStatement to prevent SQL injection.
 *  - Implements generic success message to prevent user enumeration.
 *  - Employs secure defaults: no caching, minimal information in errors.
 *  - Properly closes resources via try-with-resources.
 */
@WebServlet(name = "UnsubscribeServlet", urlPatterns = {"/unsubscribe"})
public class O4_Mini_Scenario2_Copilot8_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for security-relevant events; does not log sensitive data.
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple, safe regex for email validation (subset of RFC 5322)
        // Does not accept comments, internationalized domain names, etc.
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        );

        // JNDI name for the DataSource (configured in container, e.g., Tomcat's context.xml)
        private static final String DATASOURCE_JNDI_NAME = "java:comp/env/jdbc/MyAppDS";

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            super.init();
            try {
                // Lookup DataSource via JNDI
                InitialContext ctx = new InitialContext();
                Object ds = ctx.lookup(DATASOURCE_JNDI_NAME);
                if (!(ds instanceof DataSource)) {
                    throw new ServletException("JNDI resource is not a DataSource");
                }
                dataSource = (DataSource) ds;
            } catch (NamingException e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize DataSource", e);
                throw new ServletException("Database initialization error");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce no caching of sensitive operations
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter");
                return;
            }
            email = email.trim();

            // Validate email format
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            // Perform unsubscribe operation
            String sql = "UPDATE subscriptions SET unsubscribed = ? WHERE email = ?";
            // We do not reveal whether the email was present to avoid user enumeration
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Principle of least privilege: only write to the one column needed
                ps.setBoolean(1, true);
                ps.setString(2, email);

                int rowsAffected = ps.executeUpdate();
                // Log event at INFO level without disclosing the email in case logs are less protected
                if (rowsAffected > 0) {
                    LOGGER.log(Level.INFO, "Processed unsubscribe request");
                } else {
                    // Either email not found or already unsubscribed
                    LOGGER.log(Level.INFO, "Processed unsubscribe request (no matching record)");
                }
                // Generic response avoids user enumeration
                try (PrintWriter out = resp.getWriter()) {
                    out.println("If your email was subscribed, it has now been unsubscribed.");
                }
            } catch (SQLException e) {
                // Log full exception on server side, but return generic message to client
                LOGGER.log(Level.SEVERE, "Database error during unsubscribe", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An internal error occurred. Please try again later.");
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // For security, only GET is allowed for unsubscribe links
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "POST not supported");
        }
    }
}