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
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot6_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple RFC 5322‐lite email regex (adjust if you need more strict checking)
        private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        );

        // JNDI name of the DataSource – must be declared in web.xml or server config
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/YourDB";

        private DataSource dataSource;

        @Override
        public void init() {
            try {
                Context ctx = new InitialContext();
                // look up the DataSource you have configured in your container
                dataSource = (DataSource) ctx.lookup(DATASOURCE_JNDI);
            } catch (NamingException ne) {
                // Log a severe error and rethrow as ServletException
                log("Failed to initialize DataSource", ne);
                throw new RuntimeException("Unable to acquire DataSource", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

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

            // We perform a single DELETE; executeUpdate() returns 0 if no rows matched.
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, email);
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    // no such email in the table
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }

            } catch (SQLException sqle) {
                // Log the exception; do not expose internal messages to the client
                log("Database error while unsubscribing " + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server encountered an error.");
            }
        }
    }
}

