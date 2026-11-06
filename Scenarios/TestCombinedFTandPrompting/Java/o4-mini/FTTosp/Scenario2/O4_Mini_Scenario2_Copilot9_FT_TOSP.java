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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot9_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());
        // Simple RFC-5322 subset — adjust as needed
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
        );

        // JNDI name of your DataSource as configured in your container
        private static final String DATASOURCE_JNDI_NAME = "java:comp/env/jdbc/YourDataSource";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Get and validate email parameter
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // 2. Look up DataSource via JNDI
            DataSource ds;
            try {
                InitialContext ic = new InitialContext();
                ds = (DataSource) ic.lookup(DATASOURCE_JNDI_NAME);
            } catch (NamingException ne) {
                logger.log(Level.SEVERE, "Unable to locate DataSource via JNDI", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            // 3. Perform unsubscribe (DELETE); if no row deleted, email not found
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rowsAffected = ps.executeUpdate();

                resp.setContentType("text/plain; charset=UTF-8");
                if (rowsAffected > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }
            } catch (SQLException se) {
                logger.log(Level.SEVERE, "Database error during unsubscribe operation", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            }
        }
    }
}

