import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.IOException;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot6_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple RFC-5322‐lite email regex; adjust if you need stricter validation
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws IOException, ServletException {

            // 1. Extract and validate the 'email' parameter
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Missing 'email' parameter.");
                return;
            }
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "Invalid email format.");
                return;
            }

            // 2. Lookup DataSource via JNDI
            DataSource ds;
            try {
                Context ctx = new InitialContext();
                // this name must match your container’s resource-ref
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDB");
            } catch (NamingException ne) {
                log("Failed to look up DataSource", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server configuration error.");
                return;
            }

            // 3. Execute the unsubscribe (DELETE)
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                ps.executeUpdate();

                // 4. Uniform 200 OK response (prevents account‐enumeration)
                resp.setContentType("text/plain; charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter()
                    .write("If the address was subscribed, it has been unsubscribed.");

            } catch (SQLException se) {
                log("Database error during unsubscribe", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Database error.");
            }
        }
    }
}

