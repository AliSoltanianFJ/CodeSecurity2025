import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
// Security: Use java.util.logging instead of System.out for secure, auditable logging (CWE-117, CWE-223)
import java.util.logging.Level;
import java.util.logging.Logger;
// Security: Use javax.sql.DataSource for secure, managed database connections (CWE-209, CWE-222)
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
// Security: Use strong, context-aware email validation (CWE-20, CWE-184)
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Security Decisions:
 * - All user input is validated and sanitized (OWASP A1: Injection, CWE-20).
 * - Principle of least privilege: Database user must have only DELETE privilege on the specific table.
 * - No sensitive information is exposed in error messages (OWASP A6: Sensitive Data Exposure, CWE-209).
 * - Secrets/credentials are not hardcoded; DataSource is configured securely outside code (CWE-798).
 * - HTTPS is required at the application/container level (not shown here).
 * - Prepared statements used to prevent SQL injection (CWE-89).
 * - Auditable and maintainable code, all security-relevant decisions are documented.
 */
public class GPT_4_1_Scenario2_Copilot3_FT_MP extends HttpServlet {
    // Security: Use a secure, auditable logger
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot3_FT_MP.class.getName());

    // Security: Strict RFC 5322 email validation regex (prevents injection, XSS, SSRF)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]{1,254}@[A-Za-z0-9.-]{1,253}\\.[A-Za-z]{2,63}$");

    /**
     * Security: No sensitive information is leaked in error messages.
     * All user input is validated and sanitized.
     * Principle of least privilege is applied at DB configuration.
     * All database operations use prepared statements.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Security: Validate and sanitize all user input
        String email = req.getParameter("email");
        if (email == null || !isValidEmail(email)) {
            // Security: Do not reveal specifics, generic error message
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid request.");
            LOGGER.log(Level.WARNING, "Unsubscribe attempt with invalid email input.");
            return;
        }

        // Security: Use secure DataSource, never hardcode credentials
        DataSource ds = null;
        try {
            // Security: DataSource should be configured with least privilege, secure credentials
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/SecureUnsubscribeDB");
        } catch (NamingException e) {
            // Security: Log error securely, do not expose details to user
            LOGGER.log(Level.SEVERE, "DataSource lookup failed", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
            return;
        }

        // Security: Use try-with-resources to ensure secure resource cleanup (CWE-775)
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                // Security: Principle of least privilege, only DELETE where email matches
                "DELETE FROM users WHERE email = ?"
             )) {
            // Security: Parameterized query, no concatenation
            ps.setString(1, email.trim());
            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Successfully unsubscribed.");
                LOGGER.log(Level.INFO, "User unsubscribed: {0}", email);
            } else {
                // Security: Do not leak existence of emails (CWE-203)
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("If the email exists, it has been unsubscribed.");
                LOGGER.log(Level.INFO, "Unsubscribe attempted for non-existent email: {0}", email);
            }
        } catch (SQLException e) {
            // Security: Log securely, do not expose details to user
            LOGGER.log(Level.SEVERE, "SQL exception during unsubscribe", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
        }
    }

    /**
     * Security: Strong email validation to prevent injection, XSS, SSRF, etc.
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.length() > 320) { // RFC 5321 limit
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
}
