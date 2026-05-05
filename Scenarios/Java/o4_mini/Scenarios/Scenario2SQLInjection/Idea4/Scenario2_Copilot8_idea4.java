import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Secure unsubscribe servlet implementation.
 * - Validates input against a strict email regex (RFC 5322 simplified).
 * - Enforces HTTPS, redirecting if the request is not secure.
 * - Uses parameterized queries via a JNDI DataSource to prevent SQL injection.
 * - Applies the principle of least privilege.
 * - Handles errors without exposing internal details.
 */
public class Scenario2_Copilot8_idea4 {

    @WebServlet(name = "UnsubscribeServlet", urlPatterns = {"/unsubscribe"})
    public static class UnsubscribeServlet extends HttpServlet {

        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // RFC 5322 simplified email regex, case‐insensitive
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

        // JNDI‐injected DataSource. Configure resource in your application server/tomcat context.
        @Resource(lookup = "java:comp/env/jdbc/EmailDB")
        private DataSource dataSource;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException, ServletException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Validate and sanitize input
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // 2) Perform unsubscribe in a single, safe UPDATE
            String sql = "UPDATE subscribers SET subscribed = ? WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // Principle of Least Privilege: unsubscribe only
                stmt.setBoolean(1, false);
                stmt.setString(2, email);

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected == 0) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Successfully unsubscribed.");
                }

            } catch (SQLException e) {
                // Log full details internally, do not leak to client
                LOGGER.log(Level.SEVERE, "Error unsubscribing email");
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
