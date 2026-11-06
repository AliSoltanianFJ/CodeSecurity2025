import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot8_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple RFC-compliant-ish e-mail regex (for demo purposes)
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

        private static final Logger logger =
            Logger.getLogger(UnsubscribeServlet.class.getName());

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                // Lookup the container-provided DataSource
                InitialContext ctx = new InitialContext();
                // The JNDI name must match what's configured in your container (e.g. in web.xml or context.xml)
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDB");
            } catch (NamingException ne) {
                logger.log(Level.SEVERE, "Unable to find DataSource", ne);
                throw new ServletException("DataSource initialization failed", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            // enforce UTF-8
            req.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid or missing email address.");
                return;
            }

            // Perform unsubscribe
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                   "Email address not found.");
                }

            } catch (SQLException sqle) {
                // Log the exception, but do not expose details to the client
                logger.log(Level.SEVERE, "Database error while unsubscribing", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An internal error occurred. Please try again later.");
            }
        }
    }
}

