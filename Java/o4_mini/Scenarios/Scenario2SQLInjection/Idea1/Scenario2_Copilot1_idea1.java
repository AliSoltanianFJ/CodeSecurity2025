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

public class Scenario2_Copilot1_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        /*
         * Basic, conservative email pattern for input validation. This is intentionally
         * simplified; the goal is to reject obviously invalid input early and keep
         * downstream operations safe. Adjust as needed for your environment.
         */
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\\.[A-Za-z]{2,}$"
        );

        /**
         * Expects a JNDI DataSource bound at "java:comp/env/jdbc/SubscriberDB".
         * The servlet performs input validation, uses a PreparedStatement to avoid SQL
         * injection, avoids revealing whether an address existed (prevents enumeration),
         * and logs internal errors without exposing details to the client.
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }

            if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email parameter");
                return;
            }

            // Use JNDI DataSource to avoid hard-coded credentials in code/config.
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                Object lookup = ctx.lookup("java:comp/env/jdbc/SubscriberDB");
                if (!(lookup instanceof DataSource)) {
                    LOGGER.log(Level.SEVERE, "JNDI lookup did not return a DataSource: {0}", lookup);
                    throw new ServletException("Internal configuration error");
                }
                ds = (DataSource) lookup;
            } catch (NamingException e) {
                LOGGER.log(Level.SEVERE, "DataSource lookup failed", e);
                throw new ServletException("Internal server error");
            }

            // Perform update using PreparedStatement to prevent SQL injection.
            final String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ? AND subscribed = TRUE";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(updateSql)) {

                ps.setString(1, email);
                ps.executeUpdate();

                // Always return the same generic message to avoid account enumeration.
                try (PrintWriter out = resp.getWriter()) {
                    out.println("If an account with that email exists, it has been unsubscribed.");
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error while attempting to unsubscribe", e);
                throw new ServletException("Internal server error");
            }
        }
    }
}