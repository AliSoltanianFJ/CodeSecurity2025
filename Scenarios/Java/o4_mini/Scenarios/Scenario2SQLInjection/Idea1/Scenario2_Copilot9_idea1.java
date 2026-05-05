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
import java.util.regex.Pattern;

public class Scenario2_Copilot9_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple, conservative email pattern for basic validation
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

        /**
         * NOTE: This servlet expects a JNDI DataSource bound at: java:comp/env/jdbc/SubscriberDB
         * Configure the DataSource in your application server (web.xml / container) rather than
         * hardcoding credentials in code.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // Do a single parameterized update to avoid separate SELECT -> race/extra exposure.
            // Do not reveal whether the address existed to reduce user enumeration risk.
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/SubscriberDB");
                if (ds == null) {
                    log("DataSource lookup returned null");
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                    return;
                }

                final String sql = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";
                try (Connection conn = ds.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }

                try (PrintWriter out = resp.getWriter()) {
                    // generic response to avoid revealing whether email existed
                    out.println("If that email was subscribed, it has been unsubscribed.");
                }
            } catch (NamingException e) {
                log("JNDI lookup failed for DataSource", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
            } catch (SQLException e) {
                log("Database error while unsubscribing", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            } catch (Exception e) {
                log("Unexpected error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }
    }
}
