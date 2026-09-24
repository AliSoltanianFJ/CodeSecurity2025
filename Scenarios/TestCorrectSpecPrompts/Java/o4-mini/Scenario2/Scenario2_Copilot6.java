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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot6 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        // very simple email‐format check; you can substitute a more robust one
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid or missing email address.");
                return;
            }

            // look up pool configured under java:comp/env/jdbc/MyAppDB
            DataSource ds;
            try {
                Context ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
            } catch (NamingException ne) {
                logger.log(Level.SEVERE, "Unable to look up DataSource", ne);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal error occurred.");
                return;
            }

            String sql = "UPDATE subscriptions SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, email);
                int updated = stmt.executeUpdate();

                if (updated > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().println("You have been unsubscribed.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().println("Email not found or already unsubscribed.");
                }
            } catch (SQLException se) {
                // log full stack trace on server
                logger.log(Level.SEVERE, "Database error unsubscribing email: " + email, se);
                // but only give a generic error to the client
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal error occurred.");
            }
        }
    }
}