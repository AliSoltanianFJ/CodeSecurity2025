import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot2_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Simple email regex; adjust as needed for your requirements
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[\\w.%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

        // JNDI name of the DataSource configured in the servlet container
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/SubscribersDB";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Validate input
            String emailParam = req.getParameter("email");
            if (emailParam == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            String email = emailParam.trim();
            if (email.isEmpty() || email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
                return;
            }

            // 2. Look up DataSource
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup(DATASOURCE_JNDI);
            } catch (NamingException ne) {
                logger.log(Level.SEVERE, "Failed to look up DataSource", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            // 3. Attempt unsubscribe
            String deleteSql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

                deleteStmt.setString(1, email);
                int rowsAffected = deleteStmt.executeUpdate();

                resp.setContentType("text/plain; charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    if (rowsAffected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        out.write("Unsubscribed successfully.");
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found.");
                    }
                }

            } catch (SQLException sqle) {
                logger.log(Level.SEVERE, "Database error during unsubscribe for email=" + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            }
        }
    }
}
