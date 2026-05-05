import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot6_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());
        // Simplified RFC-style allowlist for email addresses
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                InitialContext ctx = new InitialContext();
                // Must be configured in your servlet container (e.g. Tomcat's context.xml or web.xml)
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/UnsubscribeDB");
            } catch (NamingException e) {
                LOGGER.log(Level.SEVERE, "Failed JNDI lookup for DataSource", e);
                throw new ServletException("Initialization failure");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection is required.");
                return;
            }

            // 2) Security headers
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");

            // 3) Validate input
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // 4) Perform parameterized DELETE
            String sql = "DELETE FROM subscriptions WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                ps.executeUpdate();

                // 5) Generic, idempotent response
                resp.setContentType("text/plain; charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    out.println("If the email was subscribed, it has now been unsubscribed.");
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while unsubscribing");
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service unavailable. Please try again later.");
            }
        }
    }
}
