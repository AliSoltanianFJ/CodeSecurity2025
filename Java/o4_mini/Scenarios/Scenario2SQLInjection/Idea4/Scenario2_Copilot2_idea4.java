import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@WebServlet(
    name = "UnsubscribeServlet",
    urlPatterns = {"/unsubscribe"},
    description = "Securely unsubscribes a user by email"
)
public class Scenario2_Copilot2_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // RFC-compliant basic email regex; adjust as needed
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            // Obtain DataSource via JNDI to avoid hard-coding credentials
            try {
                InitialContext ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
            } catch (NamingException ne) {
                LOGGER.log(Level.SEVERE, "DataSource lookup failed", ne);
                throw new ServletException("Cannot initialize data source", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Must be accessed over HTTPS");
                return;
            }

            resp.setContentType("text/plain;charset=UTF-8");
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // Principle of least privilege: only SELECT/UPDATE on subscribers table
            String selectSql = "SELECT id, subscribed FROM subscribers WHERE email = ?";
            String updateSql = "UPDATE subscribers SET subscribed = FALSE, unsubscribed_at = CURRENT_TIMESTAMP WHERE id = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

                selectStmt.setString(1, email);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal more than needed
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                        return;
                    }

                    long userId = rs.getLong("id");
                    boolean subscribed = rs.getBoolean("subscribed");
                    if (!subscribed) {
                        resp.getWriter().println("Email is already unsubscribed.");
                        return;
                    }

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, userId);
                        int rows = updateStmt.executeUpdate();
                        if (rows == 1) {
                            resp.getWriter().println("You have been successfully unsubscribed.");
                        } else {
                            // Should not happen; indicates unexpected state
                            LOGGER.log(Level.WARNING, "Unsubscribe update affected {0} rows for given email");
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to unsubscribe at this time");
                        }
                    }
                }

            } catch (SQLException sqle) {
                LOGGER.log(Level.SEVERE, "Database error while unsubscribing", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }
}
