// ...existing code...
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot8_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple, conservative email validation. Adjust to stricter rules if needed.
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        // JNDI name for the configured DataSource. Configure this in your servlet container.
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyDB";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email parameter.");
                return;
            }

            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                Object looked = ctx.lookup(DATASOURCE_JNDI);
                if (!(looked instanceof DataSource)) {
                    LOGGER.log(Level.SEVERE, "JNDI name {0} did not return a DataSource", DATASOURCE_JNDI);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                    return;
                }
                ds = (DataSource) looked;
            } catch (NamingException ne) {
                LOGGER.log(Level.SEVERE, "Failed to lookup DataSource: {0}", ne.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            // Update statement: unsubscribe the email and check affected rows.
            // Use a parameterized query to avoid injection and rely on the container-managed DataSource.
            String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int affected = ps.executeUpdate();

                try (PrintWriter out = resp.getWriter()) {
                    if (affected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("Unsubscribed.");
                    } else {
                        // Do not reveal whether the email existed for privacy; a distinct message is acceptable
                        // if your application policy allows it. Here we return 200 with a neutral message.
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("If that address was subscribed, it has now been unsubscribed.");
                    }
                }

            } catch (SQLException sqle) {
                // Log internal details, but return a generic error to the client.
                LOGGER.log(Level.SEVERE, "Database error while unsubscribing", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
// ...existing code...
