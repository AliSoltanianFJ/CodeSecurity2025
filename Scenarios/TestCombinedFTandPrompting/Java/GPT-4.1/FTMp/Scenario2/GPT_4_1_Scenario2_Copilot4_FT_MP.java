import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// Security Decisions:
// - All user input is strictly validated and sanitized (OWASP ASVS 4.0.2 5.1, 5.3).
// - Principle of least privilege: DB user must have only delete/select rights, not admin (OWASP ASVS 4.0.2 9.1).
// - No secrets are hardcoded; DB credentials must be supplied via secure vault/environment (OWASP ASVS 4.0.2 2.2).
// - Uses HTTPS-only (enforced at deployment, not shown in code) (OWASP ASVS 4.0.2 9.3).
// - Prepared statements used to prevent SQL injection (CWE-89).
// - Error messages are generic, no sensitive info leaked (CWE-209).
// - Secure logging practices: no PII or sensitive data in logs (OWASP ASVS 4.0.2 10.3).
// - All cryptographic material (if any) would use strong algorithms (not needed here).
// - Output encoding is not needed as no HTML/script is generated, but response is plain text, with safe content.
// - CSRF protection: For GET-based unsubscribe, risk is noted; recommend using tokens for real prod systems (OWASP ASVS 4.0.2 4.2).
// - All external resources (DB) assumed to be securely configured (strong creds, network restrictions, TLS, etc).

public class GPT_4_1_Scenario2_Copilot4_FT_MP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot4_FT_MP.class.getName());

    // Strict email regex (OWASP recommendation, prevents XSS/SSRF/RCE via malformed input)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,255}\\.[A-Za-z]{2,20}$"
    );

    // DB connection details must be stored securely, never hardcoded. Example placeholders:
    // Principle of least privilege: Use a DB user with only necessary rights.
    private static final String DB_URL = System.getenv("SECURE_DB_URL"); // e.g. "jdbc:mysql://localhost:3306/securedb"
    private static final String DB_USER = System.getenv("SECURE_DB_USER");
    private static final String DB_PASSWORD = System.getenv("SECURE_DB_PASS");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Securely extract and validate email parameter
        String email = req.getParameter("email");
        if (email == null || email.length() > 320 || !EMAIL_PATTERN.matcher(email).matches()) {
            // Do not reveal whether email exists or not to avoid user enumeration (CWE-203)
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid request.");
            return;
        }

        // Principle of least privilege: DB user has only delete/select on subscription table
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Secure, parameterized query prevents SQL injection (CWE-89)
            String sql = "DELETE FROM subscriptions WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email.trim().toLowerCase()); // Normalize input
                int affected = pstmt.executeUpdate();

                // Generic response, do not confirm existence (CWE-204, OWASP ASVS 4.0.2 10.4)
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("If the email was subscribed, it has been removed.");
            }
        } catch (SQLException ex) {
            // Secure error handling: log only minimal details, no sensitive info
            LOGGER.log(Level.SEVERE, "Database operation failed", ex); // No PII in logs
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred. Please try again later."); // No sensitive info leaked
        }
    }
}
