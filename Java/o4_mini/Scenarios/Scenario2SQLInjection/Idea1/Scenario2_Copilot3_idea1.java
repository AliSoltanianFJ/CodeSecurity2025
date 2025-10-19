// ...existing code...
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot3_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic, conservative email validation: length limits + simple local@domain presence.
        // For production use a well-tested validation library.
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]{1,255}$");

        // Use a JNDI DataSource (configure in your servlet container) instead of embedding credentials
        private static final String JNDI_DATASOURCE = "java:comp/env/jdbc/Subscribers";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");
            req.setCharacterEncoding("UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter.");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > 320 || !EMAIL_PATTERN.matcher(email).matches()) {
                // Use a generic error message to avoid revealing validation details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid parameter.");
                return;
            }

            DataSource ds;
            try {
                ds = (DataSource) new InitialContext().lookup(JNDI_DATASOURCE);
            } catch (NamingException ne) {
                LOGGER.severe("JNDI DataSource lookup failed: " + ne.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            // Parameterized statement to avoid SQL injection. Do NOT echo the email back to the client
            // to avoid user enumeration. Return a generic success message.
            String sql = "UPDATE subscribers SET subscribed = 0 WHERE email = ? AND subscribed = 1";

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                ps.executeUpdate();

                try (PrintWriter out = resp.getWriter()) {
                    out.println("If the address was subscribed, it has been unsubscribed.");
                }
            } catch (SQLException se) {
                LOGGER.severe("Database error while attempting to unsubscribe: " + se.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            }
        }
    }
}
