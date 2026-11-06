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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot3_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple email‐format check
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,6}$");
        private static final Logger logger =
                Logger.getLogger(UnsubscribeServlet.class.getName());

        // JNDI name of your DataSource
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/SubscriberDB";

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                InitialContext ic = new InitialContext();
                dataSource = (DataSource) ic.lookup(DATASOURCE_JNDI);
            } catch (NamingException e) {
                logger.log(Level.SEVERE,
                        "Failed to look up DataSource via JNDI [" + DATASOURCE_JNDI + "]", e);
                throw new ServletException(
                        "Unable to initialize unsubscribe servlet – DataSource unavailable.", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            // 1) Fetch and normalize
            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Missing required parameter: email");
                return;
            }

            // 2) Validate format
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid email format");
                return;
            }

            // 3) Attempt unsubscribe via DELETE
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, email);
                int deleted = stmt.executeUpdate();

                resp.setContentType("text/plain;charset=UTF-8");
                if (deleted > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    // email wasn’t found
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Email not found.");
                }
            } catch (SQLException e) {
                // log full stack, but give generic message to client
                logger.log(Level.SEVERE,
                        "Database error while unsubscribing: " + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Internal server error. Please try again later.");
            }
        }
    }
}

