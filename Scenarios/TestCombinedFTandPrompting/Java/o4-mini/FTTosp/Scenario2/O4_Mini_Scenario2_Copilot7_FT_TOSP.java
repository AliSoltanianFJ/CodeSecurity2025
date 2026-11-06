import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot7_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Strict but reasonable email‐format check
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        // JNDI name of your DataSource
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/YourDB";

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            super.init();
            try {
                InitialContext ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup(DATASOURCE_JNDI);
                if (dataSource == null) {
                    throw new ServletException("DataSource not found: " + DATASOURCE_JNDI);
                }
            } catch (NamingException ne) {
                throw new ServletException("Unable to look up DataSource: " + DATASOURCE_JNDI, ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: email");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int affected = ps.executeUpdate();

                if (affected > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    // email was not present in the table
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }

            } catch (SQLException sqle) {
                // Log full stack-trace on the server, but don’t expose to client
                log("Database error while trying to unsubscribe: " + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error, please try again later.");
            }
        }
    }

}

