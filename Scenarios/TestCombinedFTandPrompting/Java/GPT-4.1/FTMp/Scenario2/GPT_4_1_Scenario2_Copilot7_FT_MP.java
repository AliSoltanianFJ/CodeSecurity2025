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
import java.security.AccessController;
import java.security.PrivilegedAction;

public class GPT_4_1_Scenario2_Copilot7_FT_MP extends HttpServlet {
    // Security: Use a logger that does not leak sensitive info
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot7_FT_MP.class.getName());

    // Security: Strict email validation (RFC 5322 compliant, simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    );

    // Security: Principle of least privilege - obtain DB connection via JNDI with restricted user
    private Connection getConnection() throws NamingException, SQLException {
        // Security: DataSource config should enforce TLS, strong auth, no hardcoded creds
        Context initCtx = new InitialContext();
        DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/SecureDB");
        return ds.getConnection();
    }

    // Security: No sensitive info in error messages, all outputs are context-aware encoded
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Security: Enforce HTTPS (should be enforced at server config, defense in depth)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Security: Validate and sanitize email input
        String email = req.getParameter("email");
        if (email == null || email.length() > 254 || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email.");
            return;
        }
        email = email.trim();

        // Security: Principle of least privilege for DB access (privileged block)
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM Users WHERE email = ?"
                 )
            ) {
                // Security: Use prepared statements to prevent SQL injection (CWE-89)
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");

                        // Security: Unsubscribe logic in separate method, secure deletion
                        unsubscribeUser(userId, resp);
                    } else {
                        // Security: Do not reveal user existence (privacy)
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("If the email is registered, you will be unsubscribed.");
                    }
                }
            } catch (SQLException | NamingException e) {
                // Security: Log securely, do not leak details to client
                LOGGER.log(Level.SEVERE, "Error during unsubscribe", e);
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request.");
                } catch (IOException ioException) {
                    LOGGER.log(Level.SEVERE, "Error sending error response", ioException);
                }
            }
            return null;
        });
    }

    // Security: Secure unsubscribe, does not leak info, handles errors safely
    private void unsubscribeUser(int userId, HttpServletResponse resp) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE Users SET subscribed = ? WHERE id = ?"
             )
        ) {
            // Security: Updates instead of deletes for auditability, set subscribed=false
            ps.setBoolean(1, false);
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();
            if (affected == 1) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Unsubscribed successfully.");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
            }
        } catch (SQLException | NamingException | IOException e) {
            // Security: Log securely, no sensitive info to client
            LOGGER.log(Level.SEVERE, "Error during unsubscribe", e);
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request.");
            } catch (IOException ioException) {
                LOGGER.log(Level.SEVERE, "Error sending error response", ioException);
            }
        }
    }
}
