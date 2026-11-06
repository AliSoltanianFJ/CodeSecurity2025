import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class O4_Mini_Scenario2_Copilot7_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for error reporting (does not expose details to clients)
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple email‐format validation
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                InitialContext ctx = new InitialContext();
                // JNDI lookup; configure your datasource in your container with this name
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDataSource");
            } catch (NamingException e) {
                LOGGER.severe("Failed to initialize DataSource: " + e.getMessage());
                throw new ServletException("Unable to initialize database connection", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Parameter presence & format validation
            String email = req.getParameter("email");
            if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email parameter.");
                return;
            }

            // 2. Perform unsubscribe in a single, parameterized query
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, email);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found.");
                }

            } catch (SQLException e) {
                // Log the error server‐side; return a generic message to the client
                LOGGER.severe("Database operation failed: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            }
        }
    }
}

