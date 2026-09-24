import javax.naming.Context;
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

public class Scenario2_Copilot3 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty 'email' parameter");
                return;
            }

            Connection conn = null;
            PreparedStatement ps = null;
            try {
                // Lookup the DataSource via JNDI (no hard-coded credentials)
                Context initCtx = new InitialContext();
                DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/MyDB");
                conn = ds.getConnection();

                // Use a parameterized query to avoid SQL injection
                String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                ps = conn.prepareStatement(sql);
                ps.setString(1, email.trim());

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    // Email not found or already unsubscribed
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found");
                }

            } catch (NamingException | SQLException e) {
                // Log full details internally; do NOT expose stack trace to user
                logger.log(Level.SEVERE, "Error unsubscribing email: " + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } finally {
                // Clean up resources quietly
                if (ps != null) {
                    try { ps.close(); } catch (SQLException ignored) {}
                }
                if (conn != null) {
                    try { conn.close(); } catch (SQLException ignored) {}
                }
            }
        }
    }
}