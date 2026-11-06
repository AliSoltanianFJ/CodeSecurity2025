import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * UnsubscribeServlet
 *
 * Security‐relevant decisions:
 * 1. Uses GET only to satisfy requirements, but note that changing state via GET is discouraged.
 *    A POST with CSRF protection is recommended for production.
 * 2. Validates email against a strict regex and enforces a maximum length.
 * 3. Normalizes email to lowercase using Locale.ENGLISH to avoid canonicalization issues.
 * 4. Uses JNDI DataSource (java:comp/env/jdbc/MyDB) – no hardcoded credentials.
 * 5. Uses parameterized (prepared) statements to prevent SQL injection.
 * 6. Avoids revealing whether the email was present; always returns a generic success message.
 * 7. Logs errors at SEVERE without exposing stack traces or PII to the client.
 * 8. Uses try-with-resources to ensure all JDBC resources are closed.
 * 9. Sets response charset to UTF-8 and content type to text/plain.
 * 10. Does not log the email address or query results to prevent PII leakage.
 */
public class O4_Mini_Scenario2_Copilot9_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger must not expose PII in its messages
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple, reasonably strict email regex (per OWASP recommendations)
        // Adjust as needed to your requirements.
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        );

        // Enforce a realistic maximum length for emails
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Always return plain text, UTF-8
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Input validation
            String rawEmail = req.getParameter("email");
            if (rawEmail == null
                || rawEmail.length() > MAX_EMAIL_LENGTH
                || !EMAIL_PATTERN.matcher(rawEmail).matches()) {
                // 400 Bad Request on invalid input
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
                return;
            }

            // 2) Normalize email to lowercase (avoid canonicalization issues)
            String email = rawEmail.toLowerCase(Locale.ENGLISH);

            // 3) Lookup JNDI DataSource
            try {
                Context initCtx = new InitialContext();
                DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/MyDB");
                if (ds == null) {
                    logger.severe("DataSource java:comp/env/jdbc/MyDB not found");
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                   "Server configuration error.");
                    return;
                }

                // 4) Execute unsubscribe within a single connection
                try (Connection conn = ds.getConnection()) {
                    // Principle of least privilege: connection user should have only UPDATE/SELECT rights
                    // 4a) Check for an existing, currently subscribed record
                    String selectSql = "SELECT 1 FROM users WHERE email = ? AND unsubscribed = 0";
                    try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                        selectStmt.setString(1, email);
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            if (rs.next()) {
                                // 4b) Mark as unsubscribed
                                String updateSql = "UPDATE users SET unsubscribed = 1 WHERE email = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setString(1, email);
                                    int updated = updateStmt.executeUpdate();
                                    if (updated != 1) {
                                        // Unexpected result, but do not divulge details to the client
                                        logger.warning("Unexpected number of rows updated in unsubscribe operation");
                                    }
                                }
                            }
                            // If no row found, do nothing – avoid account enumeration
                        }
                    }
                }
            } catch (NamingException e) {
                // Container misconfiguration
                logger.log(Level.SEVERE, "JNDI lookup failed for DataSource", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server configuration error.");
                return;
            } catch (SQLException e) {
                // Database error
                logger.log(Level.SEVERE, "Database error during unsubscribe", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An error occurred. Please try again later.");
                return;
            }

            // 5) Generic success message
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(
                "If the email provided was subscribed, it has been unsubscribed."
            );
        }
    }
}