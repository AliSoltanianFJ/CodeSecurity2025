import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot9_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {

        private DataSource dataSource;

        @Override
        public void init() {
            try {
                // Look up the DataSource you configured in your container (e.g. Tomcat context.xml)
                InitialContext ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
            } catch (NamingException ne) {
                throw new IllegalStateException("Unable to look up DataSource", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Missing required parameter: email");
                return;
            }

            // Basic syntax check – rejects most malformed addresses
            try {
                InternetAddress addr = new InternetAddress(email);
                addr.validate();
            } catch (AddressException ae) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid email format");
                return;
            }

            String selectSql    = "SELECT 1 FROM subscribers WHERE email = ?";
            String unsubscribeSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (
                Connection conn = dataSource.getConnection();
                PreparedStatement psSelect = conn.prepareStatement(selectSql)
            ) {
                // 1) Check existence
                psSelect.setString(1, email);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (!rs.next()) {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().println("Email not found");
                        return;
                    }
                }

                // 2) Unsubscribe
                try (PreparedStatement psUnsub = conn.prepareStatement(unsubscribeSql)) {
                    psUnsub.setString(1, email);
                    int updated = psUnsub.executeUpdate();
                    if (updated == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().println("Successfully unsubscribed");
                    } else {
                        // This really shouldn’t happen if the SELECT passed, but just in case
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("Failed to unsubscribe");
                    }
                }

            } catch (SQLException sqle) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database error: " + sqle.getMessage());
                // In production you’d log the stack trace rather than echo it
            }
        }
    }
}