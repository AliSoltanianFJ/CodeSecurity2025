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
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot4_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        /**
         * Very simple email‐format check.
         * You may wish to tighten this regex or use a dedicated email‐validation library.
         */
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Enforce UTF-8 and plain text output
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            // 2. Read & validate parameter
            String email = req.getParameter("email");
            if (email == null || email.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // 3. Look up your container-managed DataSource (configured in web.xml or via annotations)
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                // replace "jdbc/YourDB" with the JNDI name you've configured
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDB");
            } catch (NamingException ne) {
                LOGGER.log(Level.SEVERE, "Cannot find DataSource", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal server error.");
                return;
            }

            // 4. Execute the unsubscribe (DELETE) within a try-with-resources
            final String SQL = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(SQL)) {

                ps.setString(1, email);
                int affected = ps.executeUpdate();

                if (affected > 0) {
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    // no such subscriber
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                   "Email address not found.");
                }

            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Database error while unsubscribing.", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal server error.");
            }
        }
    }
}

