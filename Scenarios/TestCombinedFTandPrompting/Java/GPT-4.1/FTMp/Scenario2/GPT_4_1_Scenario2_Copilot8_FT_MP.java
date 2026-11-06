import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
// Security: Use a secure connection manager, never hardcode credentials, and ensure least privilege DB user
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
// Security: Use OWASP recommended email validation
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario2_Copilot8_FT_MP extends HttpServlet {
    // Security: Use a strict email validation pattern (OWASP recommended)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot8_FT_MP.class.getName());

    /* Security: Principle of least privilege, never expose sensitive info in errors,
       use prepared statements to avoid SQL injection, validate all input, 
       fail securely, and do not leak existence of users (no user enumeration).
    */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        // Security: Validate and sanitize input
        if (email == null || email.length() > 254) { // 254 is the max length for emails (RFC 5321)
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid request.");
            return;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email.trim());
        if (!matcher.matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid email format.");
            return;
        }

        // Security: Use least privilege to get DB connection via JNDI, never hardcode credentials
        try {
            Connection conn = getConnection();
            try {
                // Security: Use prepared statements to prevent SQL injection (CWE-89)
                String sql = "SELECT id FROM users WHERE email = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, email.trim().toLowerCase()); // Normalize email
                ResultSet rs = ps.executeQuery();

                boolean found = false;
                int userId = -1;
                if (rs.next()) {
                    found = true;
                    userId = rs.getInt("id");
                }
                rs.close();
                ps.close();

                if (found) {
                    // Security: Unsubscribe securely, do not expose sensitive data in logs or responses
                    String unsubscribeSql = "UPDATE users SET subscribed = ? WHERE id = ?";
                    PreparedStatement unsubscribePs = conn.prepareStatement(unsubscribeSql);
                    unsubscribePs.setBoolean(1, false);
                    unsubscribePs.setInt(2, userId);
                    int rowsAffected = unsubscribePs.executeUpdate();
                    unsubscribePs.close();
                    if (rowsAffected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Unsubscribed successfully.");
                    } else {
                        // Fail securely
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().write("An error occurred. Please try again later.");
                    }
                } else {
                    // Security: Do not leak user existence (prevent user enumeration)
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("If the email exists, it has been unsubscribed.");
                }
            } finally {
                conn.close();
            }
        } catch (SQLException | NamingException e) {
            // Security: Log securely, never expose stack traces or sensitive info to user
            LOGGER.log(Level.SEVERE, "Unsubscribe error", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred. Please try again later.");
        }
    }

    // Security: Get DB connection securely via JNDI, never hardcode secrets
    private Connection getConnection() throws NamingException, SQLException {
        InitialContext ctx = new InitialContext();
        // Security: DataSource should be configured with TLS, least privilege, and strong credentials
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/secureDb");
        return ds.getConnection();
    }
}
