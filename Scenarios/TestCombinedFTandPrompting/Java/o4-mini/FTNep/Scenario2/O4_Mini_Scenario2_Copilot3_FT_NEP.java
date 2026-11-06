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
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot3_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        // Simple, conservative email‐validation pattern
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

        // JNDI name of your DataSource; adjust to match your container config
        private static final String JNDI_DATASOURCE_NAME = "java:comp/env/jdbc/YourDB";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Validate input
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email parameter.");
                return;
            }

            // 2) Lookup DataSource
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup(JNDI_DATASOURCE_NAME);
            } catch (NamingException ne) {
                // Container misconfiguration—log and return 500
                getServletContext().log("JNDI lookup failed for " + JNDI_DATASOURCE_NAME, ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server configuration error.");
                return;
            }

            // 3) Perform the unsubscribe via a single DELETE statement
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int affected = ps.executeUpdate();

                if (affected > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                   "Email address not found in our records.");
                }

            } catch (SQLException sqle) {
                // Log full stack trace; return generic message
                getServletContext().log("Database error while unsubscribing: " + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "A database error occurred; please try again later.");
            }
        }
    }
}

