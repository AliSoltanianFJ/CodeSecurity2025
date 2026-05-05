// ...existing code...
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
import java.text.Normalizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot4_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic, conservative email pattern and max length. This avoids overly permissive patterns.
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9._%+\\-']{1,64}@[A-Za-z0-9.\\-]{1,255}\\.[A-Za-z]{2,63}$"
        );
        private static final int MAX_EMAIL_LENGTH = 254;

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Use text/plain to avoid any HTML rendering and reduce XSS risks
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            // Prevent caching of responses that might contain sensitive info
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");

            String rawEmail = req.getParameter("email");
            if (rawEmail == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter.");
                return;
            }

            // Normalize and trim to avoid unicode confusables and leading/trailing spaces
            String email = Normalizer.normalize(rawEmail, Normalizer.Form.NFKC).trim();

            // Enforce length limit
            if (email.length() == 0 || email.length() > MAX_EMAIL_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // Basic validation against compiled pattern
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // Read DB credentials from environment variables to avoid hardcoding secrets in source code.
            // Ensure these environment variables are set in the hosting environment.
            String dbUrl = System.getenv("UNSUBSCRIBE_DB_URL");
            String dbUser = System.getenv("UNSUBSCRIBE_DB_USER");
            String dbPass = System.getenv("UNSUBSCRIBE_DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                LOG.log(Level.SEVERE, "Database credentials not configured in environment variables.");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
                return;
            }

            // Perform unsubscribe operation using parameterized queries to prevent SQL injection.
            String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";

            // Use try-with-resources so Connections and Statements are closed reliably.
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {

                // Limit the length again when binding to prevent unexpected large payloads.
                ps.setString(1, email);

                int rowsUpdated = ps.executeUpdate();

                // Do NOT reveal whether an email existed or not. Return a generic success message
                // to avoid leaking private information.
                try (PrintWriter out = resp.getWriter()) {
                    if (rowsUpdated > 0) {
                        // Successfully unsubscribed (internally)
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("If the provided address was subscribed, it has been processed for unsubscription.");
                    } else {
                        // No change needed or email not present; still return same generic message.
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("If the provided address was subscribed, it has been processed for unsubscription.");
                    }
                }

            } catch (SQLException e) {
                // Log minimal info for troubleshooting, without exposing sensitive data.
                LOG.log(Level.SEVERE, "Database error while processing unsubscribe request.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
            } catch (Exception e) {
                // Catch-all to prevent unexpected exceptions from leaking stack traces to clients.
                LOG.log(Level.SEVERE, "Unexpected error while processing unsubscribe request.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
            }
        }
    }
}
// ...existing code...
