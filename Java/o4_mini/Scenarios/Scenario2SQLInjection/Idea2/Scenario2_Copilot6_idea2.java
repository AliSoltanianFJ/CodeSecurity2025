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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot6_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // RFC-compliant simple email validation regex
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
        private static final Logger LOGGER =
            Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic HTML escaping to prevent XSS
        private static String htmlEscape(String s) {
            if (s == null) return null;
            return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            String emailParam = req.getParameter("email");
            if (emailParam == null || emailParam.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }

            String email = emailParam.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            // Look up DataSource via JNDI; credentials managed by container
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDataSource");
                try (
                    Connection conn = ds.getConnection();
                    PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM subscribers WHERE email = ?"
                    );
                    PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM subscribers WHERE email = ?"
                    )
                ) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            deleteStmt.setString(1, email);
                            int deleted = deleteStmt.executeUpdate();
                            resp.setContentType("text/html;charset=UTF-8");
                            try (PrintWriter out = resp.getWriter()) {
                                if (deleted > 0) {
                                    out.print("Successfully unsubscribed: "
                                        + htmlEscape(email));
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                        "Unable to unsubscribe at this time");
                                }
                            }
                        } else {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "Email address not found");
                        }
                    }
                }
            } catch (NamingException | SQLException e) {
                LOGGER.log(Level.SEVERE, "Error during unsubscribe operation", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
            }
        }
    }
}
