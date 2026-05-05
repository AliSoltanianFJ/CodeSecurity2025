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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot8_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Simple RFC-5322-lite email validation
        private static final Pattern EMAIL_REGEX =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'email' parameter.");
                return;
            }
            email = email.trim();
            if (!EMAIL_REGEX.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            try (Connection conn = getDataSource().getConnection()) {
                // 1) Check existence
                String checkSql = "SELECT subscribed FROM subscribers WHERE email = ?";
                try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                    psCheck.setString(1, email);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (!rs.next()) {
                            // email not found
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                            return;
                        }
                        boolean currentlySubscribed = rs.getBoolean("subscribed");
                        if (!currentlySubscribed) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().write("Email is already unsubscribed.");
                            return;
                        }
                    }
                }

                // 2) Unsubscribe
                String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                try (PreparedStatement psUnsub = conn.prepareStatement(updateSql)) {
                    psUnsub.setString(1, email);
                    int rows = psUnsub.executeUpdate();
                    if (rows == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Successfully unsubscribed.");
                    } else {
                        // Shouldn't really happen—indicates a logic or data integrity issue
                        getServletContext().log("Unexpected update count for email=" + email + ": " + rows);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                       "Could not unsubscribe at this time.");
                    }
                }

            } catch (SQLException | NamingException e) {
                // Log the error, but don’t expose details to the client
                getServletContext().log("Database error while unsubscribing email=" + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server error processing your request.");
            }
        }

        /**
         * Looks up the DataSource via JNDI.  
         * Make sure you have a <resource-ref> or <env-entry> configured,
         * e.g. in your web.xml or your application server’s config:
         *
         * <resource-ref>
         *   <description>My application’s DB</description>
         *   <res-ref-name>jdbc/MyDB</res-ref-name>
         *   <res-type>javax.sql.DataSource</res-type>
         *   <res-auth>Container</res-auth>
         * </resource-ref>
         */
        private DataSource getDataSource() throws NamingException {
            Context initCtx = new InitialContext();
            // adjust "java:comp/env/jdbc/MyDB" to match your JNDI name
            return (DataSource) initCtx.lookup("java:comp/env/jdbc/MyDB");
        }
    }
}