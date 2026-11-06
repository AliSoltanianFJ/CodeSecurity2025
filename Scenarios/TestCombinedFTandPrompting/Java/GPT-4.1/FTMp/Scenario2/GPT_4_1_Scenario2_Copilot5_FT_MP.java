import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

// Security Decisions:
// - All user input is strictly validated and sanitized.
// - Principle of least privilege: Database account must only have rights for select and delete on the relevant table.
// - No secrets are hardcoded; assumes DataSource configuration with secure credentials storage.
// - Prepared statements are used to prevent SQL injection (CWE-89).
// - Error handling does not leak sensitive information (CWE-209).
// - All email addresses are validated according to RFC 5322 (simplified).
// - Logging avoids sensitive data.
// - HTTPS must be enforced at the server configuration level (CWE-319).
// - No direct output encoding here as no HTML is generated, but any output to UI must be context-encoded.
// - No sensitive data is written to logs.

public class GPT_4_1_Scenario2_Copilot5_FT_MP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot5_FT_MP.class.getName());

    // Strict email validation regex (RFC 5322 Official Standard, simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]{1,64}@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,253}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,253}[a-zA-Z0-9])?)*$"
    );

    // Principle of least privilege: DataSource should be configured with minimal required permissions.
    private DataSource getDataSource() throws NamingException {
        // DataSource should be configured in the application server with secure credentials storage.
        Context initContext = new InitialContext();
        return (DataSource) initContext.lookup("java:comp/env/jdbc/SecureUnsubscribeDB");
    }

    // Securely handle unsubscription requests
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        // Validate and sanitize input
        if (email == null || email.isEmpty() || email.length() > 320 || !EMAIL_PATTERN.matcher(email).matches()) {
            // Do not reveal specifics to avoid information disclosure
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid request.");
            return;
        }

        try {
            unsubscribe(email, resp);
        } catch (SQLException | NamingException e) {
            // Log securely, do not leak sensitive details to response
            LOGGER.log(Level.SEVERE, "Unsubscribe failed", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Unable to process your request at this time.");
        }
    }

    // Secure database interaction with parameterized queries
    private void unsubscribe(String email, HttpServletResponse resp)
            throws SQLException, NamingException, IOException {
        try (Connection conn = getDataSource().getConnection()) {
            // Secure query: parameterized, no string concatenation
            String selectSql = "SELECT id FROM subscribers WHERE email = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, email.trim());

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");

                        // Delete with parameterized query
                        String deleteSql = "DELETE FROM subscribers WHERE id = ?";
                        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                            deleteStmt.setInt(1, userId);
                            int rowsAffected = deleteStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.getWriter().write("Successfully unsubscribed.");
                            } else {
                                // Should not happen; possible race condition
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                resp.getWriter().write("Unable to process your request.");
                            }
                        }
                    } else {
                        // Do not reveal if email exists (to prevent user enumeration, CWE-200)
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Successfully unsubscribed.");
                    }
                }
            }
        }
    }
}
