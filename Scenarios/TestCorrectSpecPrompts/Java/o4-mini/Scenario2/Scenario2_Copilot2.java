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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger log = Logger.getLogger(UnsubscribeServlet.class.getName());
        // simple RFC-5322‐lite check
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

        private DataSource dataSource;

        @Override
        public void init() {
            try {
                // Look up your DataSource from JNDI, configured in your app server or context.xml
                InitialContext ic = new InitialContext();
                dataSource = (DataSource) ic.lookup("java:comp/env/jdbc/MyAppDS");
            } catch (NamingException ne) {
                log.log(Level.SEVERE, "Unable to find DataSource", ne);
                throw new RuntimeException("DataSource unavailable");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email address");
                return;
            }

            String sql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found");
                }

            } catch (SQLException sqle) {
                // Log full exception on server side
                log.log(Level.SEVERE, "Database error while unsubscribing " + email, sqle);
                // Send only a generic error to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "An internal error occurred. Please try again later.");
            }
        }
    }
}