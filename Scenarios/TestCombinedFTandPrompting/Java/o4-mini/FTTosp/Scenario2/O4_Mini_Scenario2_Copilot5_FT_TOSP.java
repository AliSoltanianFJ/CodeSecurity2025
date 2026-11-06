import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class O4_Mini_Scenario2_Copilot5_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for server‐side diagnostics
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic email pattern; adjust/restrict as your policy requires
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            // 1) Validate input
            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()
                    || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing email parameter.");
                return;
            }

            // 2) Lookup DataSource via JNDI (configured in your app server/context.xml)
            DataSource ds;
            try {
                Context initCtx = new InitialContext();
                Context envCtx  = (Context) initCtx.lookup("java:comp/env");
                ds = (DataSource) envCtx.lookup("jdbc/SubscriberDB");
            } catch (NamingException ne) {
                logger.severe("JNDI DataSource lookup failed: " + ne.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error.");
                return;
            }

            // 3) Perform unsubscribe via a single DELETE statement
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (
                Connection conn       = ds.getConnection();
                PreparedStatement ps  = conn.prepareStatement(sql)
            ) {
                ps.setString(1, email);
                int rows = ps.executeUpdate();

                try (PrintWriter out = resp.getWriter()) {
                    if (rows > 0) {
                        out.write("Unsubscribed successfully.");
                    } else {
                        // No such email
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        out.write("Email not found.");
                    }
                }
            } catch (SQLException sqle) {
                logger.severe("Database error during unsubscribe: " + sqle.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            }
        }
    }
}

