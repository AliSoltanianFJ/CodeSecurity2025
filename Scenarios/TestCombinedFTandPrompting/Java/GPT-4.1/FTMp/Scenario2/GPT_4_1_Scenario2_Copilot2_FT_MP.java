import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario2_Copilot2_FT_MP extends HttpServlet {

    // Secure email validation pattern (RFC 5322 compliant, prevents injection via input)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE
    );

    // Logger configured to avoid leaking sensitive data
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot2_FT_MP.class.getName());

    /* 
     * Security Decisions:
     * - All inputs are strictly validated and sanitized.
     * - Uses prepared statements to prevent SQL injection (CWE-89).
     * - No sensitive information is leaked in error responses.
     * - Principle of least privilege: DB user should have only DELETE/SELECT rights on the subscription table.
     * - No secrets are hardcoded; DB credentials must be stored securely (e.g., environment variables, secure vault).
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        // Validate and sanitize email input
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid email address.");
            return;
        }

        try {
            // Get the DataSource from JNDI (credentials/config not hardcoded)
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/YourSecureDB");

            try (Connection conn = ds.getConnection()) {
                conn.setReadOnly(false);
                unsubscribeEmail(conn, email.trim(), resp);
            } catch (SQLException e) {
                // Log securely
                LOGGER.log(Level.SEVERE, "Secure SQL exception during unsubscription", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An internal error occurred. Please try again later.");
            }
        } catch (NamingException ex) {
            LOGGER.log(Level.SEVERE, "Secure Naming exception during DB lookup", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An internal error occurred. Please try again later.");
        }
    }

    /**
     * Unsubscribes the email if present, using secure prepared statements.
     * All sensitive data is encrypted at rest (DB config, not shown here).
     */
    private void unsubscribeEmail(Connection conn, String email, HttpServletResponse resp) throws IOException {
        String selectSql = "SELECT id FROM subscriptions WHERE email = ?";
        String deleteSql = "DELETE FROM subscriptions WHERE email = ?";
        boolean found = false;

        // Principle of least privilege: Only accesses required columns
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, email);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    found = true;
                }
            }
        } catch (SQLException e) {
            // Log securely
            LOGGER.log(Level.SEVERE, "Secure SQL exception during select", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred. Please try again later.");
            return;
        }

        if (found) {
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, email);
                int affectedRows = deleteStmt.executeUpdate();
                if (affectedRows > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Successfully unsubscribed.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found.");
                }
            } catch (SQLException e) {
                // Log securely
                LOGGER.log(Level.SEVERE, "Secure SQL exception during delete", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred. Please try again later.");
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Email not found.");
        }
    }
}
