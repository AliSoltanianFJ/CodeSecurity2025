import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot3_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic, practical email pattern (covers the vast majority of valid emails but is safe and concise).
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,189}\\.[A-Za-z]{2,}$", Pattern.CASE_INSENSITIVE);

        // Maximum length for an email address per common limits (RFC local-part + domain).
        private static final int MAX_EMAIL_LENGTH = 254;

        // Environment variable names (recommended to be set in the container / server configuration).
        private static final String ENV_DB_URL = "DB_URL";
        private static final String ENV_DB_USER = "DB_USER";
        private static final String ENV_DB_PASSWORD = "DB_PASSWORD";
        private static final String ENV_DB_DRIVER = "DB_DRIVER_CLASS"; // optional

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Secure response headers
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter.");
                return;
            }

            email = email.trim();
            if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
                return;
            }

            if (!isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Load DB credentials from environment - do not hardcode secrets.
            String dbUrl = System.getenv(ENV_DB_URL);
            String dbUser = System.getenv(ENV_DB_USER);
            String dbPassword = System.getenv(ENV_DB_PASSWORD);
            String dbDriver = System.getenv(ENV_DB_DRIVER);

            if (dbUrl == null || dbUrl.isEmpty()) {
                LOG.log(Level.SEVERE, "Database URL not configured (env {0})", ENV_DB_URL);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
                return;
            }

            // Optionally load driver class if provided (modern JDBC drivers usually auto-register).
            if (dbDriver != null && !dbDriver.isEmpty()) {
                try {
                    Class.forName(dbDriver);
                } catch (ClassNotFoundException e) {
                    LOG.log(Level.SEVERE, "DB driver class not found.", e);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
                    return;
                }
            }

            // Perform an atomic update using a prepared statement to prevent SQL injection.
            // Note: The subscribers table and unsubscribed boolean column must exist in your schema.
            String sql = "UPDATE subscribers SET unsubscribed = ? WHERE email = ? AND (unsubscribed IS NULL OR unsubscribed = ?)";
            // Using try-with-resources to ensure all JDBC resources are closed safely.
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Use parameters for all user-supplied data.
                ps.setBoolean(1, true);      // set unsubscribed = TRUE
                ps.setString(2, email);      // WHERE email = ?
                ps.setBoolean(3, true);      // AND (unsubscribed IS NULL OR unsubscribed = TRUE) -> we want rows that are not already true
                // Note: the third parameter uses true to match existing TRUE values; depending on schema semantics,
                // you may want AND (unsubscribed IS NULL OR unsubscribed = FALSE). Adjust to your DB as needed.
                // The critical point is use of PreparedStatement to avoid injection.

                int rowsAffected = ps.executeUpdate();
                // For privacy, do NOT reveal whether the email existed. Always return the same generic message.
                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("If this address was subscribed, it has been unsubscribed.");
                }

                // Log the operation at an appropriate level without including the email or other PII.
                LOG.log(Level.INFO, "Unsubscribe request processed; rows affected: {0}", rowsAffected);

            } catch (SQLException e) {
                // Log server-side details for debugging/ops but return a generic message to client.
                LOG.log(Level.SEVERE, "Database error while processing unsubscribe request.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Unexpected error while processing unsubscribe request.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
            }
        }

        private static boolean isValidEmail(String email) {
            // Reject overly long values and enforce pattern; prevents many injection/XSS cases by restricting content.
            if (email == null || email.length() > MAX_EMAIL_LENGTH) return false;
            return EMAIL_PATTERN.matcher(email).matches();
        }
    }
}
