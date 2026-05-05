import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot2_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Simple email‐format check. Tweak as needed.
        private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

        private DataSource dataSource;

        @Override
        public void init() {
            try {
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                // Change "jdbc/YourDataSource" to your JNDI name
                dataSource = (DataSource) envCtx.lookup("jdbc/YourDataSource");
            } catch (NamingException e) {
                throw new IllegalStateException("Unable to retrieve DataSource", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || !EMAIL_REGEX.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email parameter");
                return;
            }

            // Unsubscribe by setting subscribed = FALSE (or however your schema works)
            String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rowsUpdated = ps.executeUpdate();

                if (rowsUpdated > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("You have been unsubscribed successfully.");
                } else {
                    // No such email in the database
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                }

            } catch (SQLException e) {
                log("Database error while attempting to unsubscribe email: " + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An internal error occurred. Please try again later.");
            }
        }
    }
}
