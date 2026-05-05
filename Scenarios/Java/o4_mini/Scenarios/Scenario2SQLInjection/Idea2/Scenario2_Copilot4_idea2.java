import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot4_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // RFC-compliant basic email regex, max length 254
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\\.[A-Za-z]{2,6}$"
        );

        // Load JDBC driver if needed (MySQL example)
        static {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new ExceptionInInitializerError("JDBC Driver not found: " + e.getMessage());
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty() || email.length() > 254
                    || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            // get DB connection details from environment (do not hardcode credentials)
            String dbUrl  = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error");
                return;
            }

            String updateSql = "UPDATE subscriptions SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {

                stmt.setString(1, email);
                int rows = stmt.executeUpdate();

                resp.setContentType("text/plain; charset=UTF-8");
                if (rows > 0) {
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    resp.getWriter().write("Email not found or already unsubscribed.");
                }

            } catch (SQLException e) {
                // log the exception on server side, do not expose details to client
                log("Database error while unsubscribing email: " + e.getMessage(), e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }
    }
}
