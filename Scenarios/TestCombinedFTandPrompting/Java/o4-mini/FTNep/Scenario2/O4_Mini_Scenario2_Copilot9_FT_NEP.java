import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class O4_Mini_Scenario2_Copilot9_FT_NEP {

    @WebServlet(name = "UnsubscribeServlet", urlPatterns = "/unsubscribe")
    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Inject your container-managed DataSource via JNDI
        @Resource(lookup = "java:comp/env/jdbc/SubscribersDB")
        private DataSource dataSource;

        // Simple RFC-inspired email pattern
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Parameter presence & basic normalization
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing.");
                return;
            }
            email = email.trim();

            // 2) Basic email‐format validation
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // 3) Perform the unsubscribe
            //    We combine existence check + delete in one SQL DELETE
            final String DELETE_SQL = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
                 
                ps.setString(1, email);
                int affected = ps.executeUpdate();

                if (affected > 0) {
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    // No rows → email wasn’t in the subscribers table
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }

            } catch (SQLException e) {
                // Log the full exception on the server, but don’t expose details to the client
                log("Database error unsubscribing email: " + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal server error.");
            }
        }
    }
}

