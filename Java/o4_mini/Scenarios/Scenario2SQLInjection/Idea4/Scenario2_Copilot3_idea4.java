package Scenarios.Scenario2SQLInjection;

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

public class Scenario2_Copilot3_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for internal error tracking (do not expose stack traces to users)
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Strict email‐format regex (OWASP recommended pattern)
        private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        // JNDI DataSource (configured in your servlet container with minimal privileges)
        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            super.init();
            try {
                InitialContext ctx = new InitialContext();
                // Configure "java:comp/env/jdbc/EmailDB" in your app server
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/EmailDB");
            } catch (NamingException e) {
                LOGGER.log(Level.SEVERE, "DataSource JNDI lookup failed", e);
                throw new ServletException("Initialization error");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Require HTTPS to protect data in transit
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
                return;
            }

            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("application/json");

            // Fetch and trim parameter
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Email parameter is missing\"}");
                return;
            }
            email = email.trim();

            // Validate format
            if (!EMAIL_REGEX.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"Invalid email format\"}");
                return;
            }

            // Prepare and execute the unsubscribe update using a prepared statement
            final String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int updatedRows = ps.executeUpdate();

                try (PrintWriter out = resp.getWriter()) {
                    if (updatedRows > 0) {
                        out.write("{\"status\":\"success\"}");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.write("{\"error\":\"Email not found\"}");
                    }
                }
            } catch (SQLException e) {
                // Log internal error, but return a generic message
                LOGGER.log(Level.SEVERE, "Database error during unsubscribe", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"error\":\"Internal server error\"}");
            }
        }
    }
}
