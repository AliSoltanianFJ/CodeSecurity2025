import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Security Decisions:
 * - Uses parameterized queries to prevent SQL injection (CWE-89).
 * - Validates and sanitizes all user input (email).
 * - No sensitive information is leaked in error messages.
 * - Principle of least privilege: assumes DB user has only necessary rights.
 * - No secrets or credentials are hardcoded; uses JNDI for DB connection.
 * - All operations are logged securely, no sensitive data in logs.
 * - Assumes HTTPS is enforced at the server/container level.
 * - Follows OWASP and CERT Java Secure Coding Guidelines.
 */
public class GPT_4_1_Scenario2_Copilot6_FT_MP extends HttpServlet {
    // Secure email validation regex (RFC 5322 compliant, but restricts to common safe subset)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,255}\\.[A-Za-z]{2,63}$"
    );

    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot6_FT_MP.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // Enforce HTTPS (defense in depth, should also be enforced at server config)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
            return;
        }

        // Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            // Do not reveal specifics to avoid information disclosure (CWE-209)
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
            return;
        }

        // Principle of least privilege: DB operations should use least privileged user
        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException | NamingException ex) {
            // Log securely, do not expose stack trace or sensitive info to user
            LOGGER.log(Level.SEVERE, "Unsubscription failed due to server error.");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process your request at this time.");
        }
    }

    /**
     * Validates email using a strict regex.
     * @param email input email
     * @return true if valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Unsubscribes the user if the email exists in the database.
     * Uses parameterized queries to prevent SQL injection.
     * @param email sanitized and validated email
     * @param resp HttpServletResponse for sending result
     * @throws SQLException if DB error occurs
     * @throws NamingException if JNDI lookup fails
     * @throws IOException if response fails
     */
    private void unsubscribeEmail(String email, HttpServletResponse resp)
            throws SQLException, NamingException, IOException {
        // Get secure DataSource from JNDI (no hardcoded credentials)
        DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/SecureDB");
        try (Connection conn = ds.getConnection()) {
            // Principle of least privilege: ensure DB user has only DELETE rights on subscription table
            String selectSql = "SELECT id FROM subscriptions WHERE email = ?";
            String deleteSql = "DELETE FROM subscriptions WHERE email = ?";

            // Check if email exists
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, email.trim());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        // Email exists, proceed to delete
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setString(1, email.trim());
                            int rows = deleteStmt.executeUpdate();
                            if (rows > 0) {
                                // Success, send generic success message
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.getWriter().write("Unsubscribed successfully.");
                            } else {
                                // Should not happen, but handle gracefully
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to unsubscribe.");
                            }
                        }
                    } else {
                        // Do not reveal if email exists (prevents user enumeration, CWE-203)
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Unsubscribed successfully.");
                    }
                }
            }
        }
    }
}
