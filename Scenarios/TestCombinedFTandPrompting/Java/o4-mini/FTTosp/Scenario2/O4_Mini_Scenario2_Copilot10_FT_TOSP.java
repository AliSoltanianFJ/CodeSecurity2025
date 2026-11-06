import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@WebServlet("/unsubscribe")
public class O4_Mini_Scenario2_Copilot10_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());
        // Simple email‐validation regex; adjust as needed
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                Context ctx = new InitialContext();
                // Look up your DataSource via JNDI (configured in your application server or context.xml)
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDB");
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Failed to initialize DataSource", e);
                throw new ServletException("Unable to initialize DataSource", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce UTF-8
            req.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain;charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is required.");
                return;
            }
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Use a single DELETE statement and check affected rows
            String sql = "DELETE FROM subscribers WHERE email = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, email);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error during unsubscribe operation", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
