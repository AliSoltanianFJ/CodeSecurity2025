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

public class Scenario2_Copilot4 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Read & validate
            String email = req.getParameter("email");
            if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email address.");
                return;
            }

            // 2) Lookup DataSource via JNDI (no hard-coded credentials here)
            DataSource ds;
            try {
                Context initCtx = new InitialContext();
                // The resource "jdbc/MyAppDB" must be configured in your servlet container
                ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/MyAppDB");
            } catch (NamingException ne) {
                log("JNDI lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            // 3) Execute parameterized update
            String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found.");
                }

            } catch (SQLException se) {
                log("Database error while unsubscribing", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request.");
            }
        }
    }
}