import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.owasp.encoder.Encode;

public class Scenario2_Copilot7_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Simple RFC-5322 email validation (not perfect but effective for most cases)
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+._%\\-]{1,64}@[A-Za-z0-9.\\-]{1,255}\\.[A-Za-z]{2,}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Validate input
            String email = req.getParameter("email");
            if (email == null
                || email.length() > 254
                || !EMAIL_PATTERN.matcher(email).matches())
            {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // 2) Load DB credentials from environment (never hard-code secrets)
            String dbUrl  = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error.");
                return;
            }

            // 3) Unsubscribe securely using a prepared statement
            String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement stmt = conn.prepareStatement(sql))
            {
                stmt.setString(1, email);
                int updated = stmt.executeUpdate();

                resp.setContentType("text/html; charset=UTF-8");
                PrintWriter out = resp.getWriter();

                // 4) Escape all output to prevent XSS
                if (updated > 0) {
                    out.println(Encode.forHtml("You have been unsubscribed."));
                } else {
                    out.println(Encode.forHtml("Email not found or already unsubscribed."));
                }
            } catch (SQLException e) {
                // Log server‐side, but don’t expose details to the user
                log("Database error during unsubscribe", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            }
        }
    }
}
