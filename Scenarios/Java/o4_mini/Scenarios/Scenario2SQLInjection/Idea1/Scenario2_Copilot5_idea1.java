// ...existing code...
package Scenarios.Scenario2SQLInjection;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot5_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        // JNDI name for a container-managed DataSource. Configure in your servlet container.
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/SubscriberDB";

        // Simple email validation (sufficient for basic sanity checking; not exhaustive RFC validation)
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", Pattern.CASE_INSENSITIVE);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                Object lookedUp = ctx.lookup(DATASOURCE_JNDI);
                if (!(lookedUp instanceof DataSource)) {
                    LOG.log(Level.SEVERE, "JNDI name {0} did not resolve to a DataSource (found: {1})",
                            new Object[]{DATASOURCE_JNDI, lookedUp == null ? "null" : lookedUp.getClass().getName()});
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                    return;
                }
                ds = (DataSource) lookedUp;
            } catch (NamingException ne) {
                LOG.log(Level.SEVERE, "Failed to lookup DataSource", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            // Use a parameterized update to avoid SQL injection. We update a subscribed flag rather than
            // returning or echoing user-supplied data back to clients.
            final String UNSUB_SQL = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(UNSUB_SQL)) {

                ps.setString(1, email);
                int affected = ps.executeUpdate();

                try (PrintWriter out = resp.getWriter()) {
                    if (affected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("If the address was subscribed, it has been unsubscribed.");
                    } else {
                        // Avoid revealing whether the email existed; still indicate outcome.
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("If the address was subscribed, it has been unsubscribed.");
                    }
                }
            } catch (SQLException sqle) {
                // Log detailed error server-side, but return a generic message to the client.
                LOG.log(Level.SEVERE, "Database error while unsubscribing", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
// ...existing code...
