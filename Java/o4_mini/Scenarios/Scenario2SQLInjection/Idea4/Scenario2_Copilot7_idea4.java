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

public class Scenario2_Copilot7_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // RFC 5322 simplified email validation
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        private DataSource lookupDataSource() throws NamingException {
            InitialContext ctx = new InitialContext();
            // DataSource must be configured in your application server under this JNDI name
            return (DataSource) ctx.lookup("java:comp/env/jdbc/SubscribersDB");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            DataSource ds;
            try {
                ds = lookupDataSource();
            } catch (NamingException ne) {
                LOGGER.log(Level.SEVERE, "Failed to obtain DataSource", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
                return;
            }

            String sql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                int rows = ps.executeUpdate();
                try (PrintWriter out = resp.getWriter()) {
                    if (rows > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.println("You have been unsubscribed successfully.");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        // Do not reveal whether the email existed or not to prevent enumeration
                        out.println("If this email was subscribed, you have now been unsubscribed.");
                    }
                }
            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Database error during unsubscription", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            }
        }
    }
}
