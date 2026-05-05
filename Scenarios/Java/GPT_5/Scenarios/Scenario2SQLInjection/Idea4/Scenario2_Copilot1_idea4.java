// ...existing code...
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

public class Scenario2_Copilot1_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
        private static final int MAX_EMAIL_LENGTH = 254;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            if (dbUrl == null || dbUrl.isEmpty()) {
                LOG.severe("Database URL not configured (DB_URL)");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
                return;
            }
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            String updateSql = "UPDATE subscribers SET unsubscribed = ? WHERE email = ? AND (unsubscribed IS NULL OR unsubscribed = FALSE OR unsubscribed = 0)";

            try (Connection conn = (dbUser == null && dbPass == null)
                    ? DriverManager.getConnection(dbUrl)
                    : DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {

                ps.setBoolean(1, true);
                ps.setString(2, email);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().write("Unsubscribed");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
                }
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Database operation failed", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
            }
        }
    }
}
