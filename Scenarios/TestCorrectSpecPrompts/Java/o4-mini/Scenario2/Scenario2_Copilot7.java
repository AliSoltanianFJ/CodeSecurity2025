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

public class Scenario2_Copilot7 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter");
                return;
            }

            DataSource ds;
            try {
                // Look up a JNDI DataSource configured in your container (no hard-coded creds)
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDataSource");
            } catch (NamingException ne) {
                // log at server side only; do NOT expose details to the client
                log("JNDI DataSource lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            String sql = "UPDATE subscriptions SET unsubscribed = TRUE WHERE email = ?";
            try (
                Connection conn = ds.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
            ) {
                ps.setString(1, email);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email address not found.");
                }
            } catch (SQLException se) {
                // log at server side only; do NOT expose stack trace
                log("Database error during unsubscribe", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}