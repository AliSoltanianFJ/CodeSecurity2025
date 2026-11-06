import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class O4_Mini_Scenario2_Copilot4_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = LoggerFactory.getLogger(UnsubscribeServlet.class);

        // Simple RFC-5322-light email regex
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
        );

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            // Lookup the DataSource via JNDI. Configure "jdbc/YourDB" in web.xml or your app server.
            try {
                InitialContext ctx = new InitialContext();
                // adjust the name to match your JNDI configuration
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDB");
            } catch (NamingException ne) {
                logger.error("Unable to initialize DataSource", ne);
                throw new ServletException("DataSource init failed", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Retrieve and validate parameter
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing or empty.");
                return;
            }
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // 2) Attempt to delete; use one statement rather than separate SELECT + DELETE
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    // Successfully unsubscribed
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    // No such email in the database
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }

            } catch (SQLException sqle) {
                // Log full stack trace server-side, but send a generic message to the client
                logger.error("Database error while unsubscribing email: {}", email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error. Please try again later.");
            }
        }
    }
}

