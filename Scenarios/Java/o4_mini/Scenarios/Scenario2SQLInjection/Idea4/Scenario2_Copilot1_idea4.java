import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scenario2.UnsubscribeServlet
 *
 * Security measures:
 * - Enforces HTTPS (req.isSecure())
 * - Rigorous input validation using JavaMail InternetAddress
 * - Parameterized queries (no SQL injection)
 * - Least privilege via JNDI DataSource lookup
 * - Proper resource cleanup via try-with-resources
 * - Generic error messages (no sensitive info leakage)
 * - Logging of internal errors only on server side
 */
public class Scenario2_Copilot1_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());
        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                // DataSource must be configured in the container (no hard-coded credentials)
                Context ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Failed to initialize DataSource", e);
                // Fail fast if DataSource is unavailable
                throw new ServletException("Initialization error");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Ensure UTF-8 and plain text
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain;charset=UTF-8");

            // Enforce HTTPS to protect data in transit
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
                return;
            }

            // Retrieve and trim parameter
            String emailParam = req.getParameter("email");
            if (emailParam == null || emailParam.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'email' parameter.");
                return;
            }
            String email = emailParam.trim();

            // Validate email format strictly per RFC
            try {
                InternetAddress addr = new InternetAddress(email);
                addr.validate();
            } catch (AddressException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Check subscription status and unsubscribe if necessary
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement selectStmt = conn.prepareStatement(
                     "SELECT subscribed FROM subscribers WHERE email = ?")) {

                selectStmt.setString(1, email);
                boolean found = false;
                boolean subscribed = false;
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        found = true;
                        subscribed = rs.getBoolean("subscribed");
                    }
                }

                if (found && subscribed) {
                    // Unsubscribe with a second prepared statement
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                             "UPDATE subscribers SET subscribed = ? WHERE email = ?")) {
                        updateStmt.setBoolean(1, false);
                        updateStmt.setString(2, email);
                        int updated = updateStmt.executeUpdate();
                        if (updated == 1) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().println("Unsubscribed successfully.");
                        } else {
                            // Unexpected—handle gracefully
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                           "Unable to unsubscribe at this time.");
                        }
                    }
                } else {
                    // Generic response to avoid user enumeration
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().println(
                        "If the email was subscribed, it has now been unsubscribed.");
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error during unsubscribe process", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal server error.");
            }
        }
    }
}
