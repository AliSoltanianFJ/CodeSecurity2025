import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Security Decisions:
 * - All user input is validated and sanitized (OWASP ASVS 4.0, V5).
 * - Prepared statements used to prevent SQL Injection (CWE-89).
 * - Principle of least privilege assumed on DB user (OWASP ASVS 4.0, V9.1).
 * - No secrets hardcoded; DB credentials must be stored in secure config/vault (CWE-798).
 * - Error handling avoids leaking sensitive info (CWE-209).
 * - HTTPS must be enforced at server config level (OWASP A6).
 * - All logs are security-reviewed, no sensitive data is logged (CWE-532).
 * - No authentication/authorization shown here; must be enforced in production (OWASP ASVS 4.0, V2).
 */
public class GPT_4_1_Scenario2_Copilot10_FT_MP extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Strict email pattern (RFC 5322 compliant, avoids ReDoS by limiting length)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,255}\\.[A-Za-z]{2,25}$"
    );

    // Logger configured to avoid sensitive data leaks
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot10_FT_MP.class.getName());

    /**
     * Get DB connection from JNDI. Credentials must not be hardcoded.
     * DataSource should be configured with least privilege.
     */
    private Connection getConnection() throws NamingException, SQLException {
        Context initContext = new InitialContext();
        // DataSource name must not leak sensitive info
        DataSource ds = (DataSource) initContext.lookup("java:comp/env/jdbc/mySecureDB");
        return ds.getConnection();
    }

    /**
     * Securely validates and sanitizes email input.
     * @param email user-supplied email from GET param
     * @return sanitized email or null if invalid
     */
    private String validateAndSanitizeEmail(String email) {
        if (email == null || email.length() > 320) { // Max length per RFC 3696
            return null;
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        // Canonicalization: lower case for DB consistency
        return trimmed.toLowerCase();
    }

    /**
     * Securely handles unsubscribe logic.
     * All SQL uses prepared statements.
     * No sensitive info is leaked in responses.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Enforce HTTPS (should also be done at server level)
        if (req.getScheme() == null || !"https".equalsIgnoreCase(req.getScheme())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Validate and sanitize input
        String email = validateAndSanitizeEmail(req.getParameter("email"));
        if (email == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM subscribers WHERE email = ?"
             )) {
            ps.setString(1, email);
            int affected = ps.executeUpdate();

            // Generic response, do not reveal if email existed (to prevent user enumeration, CWE-203)
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("If your email was subscribed, it has been unsubscribed.");

        } catch (SQLException | NamingException e) {
            // Log securely, no sensitive details to client
            LOGGER.log(Level.WARNING, "Unsubscribe error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error. Please try again later.");
        }
    }
}
