import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario2_Copilot10 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter.");
                return;
            }

            // simple email format check (you might want a more robust regex or library)
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                // look up your JDBC DataSource via JNDI; configure in your app server / context.xml
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/SubscriberDB");
            } catch (NamingException ne) {
                LOGGER.log(Level.SEVERE, "DataSource lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
                return;
            }

            String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email address not found.");
                }
            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Database error while unsubscribing " + email, se);
                // Do NOT expose se.getMessage() or stack trace to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}