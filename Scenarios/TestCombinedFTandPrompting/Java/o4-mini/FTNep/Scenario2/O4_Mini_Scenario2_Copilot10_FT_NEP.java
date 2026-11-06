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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot10_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // A simple (RFC 5322-ish) email regex, case-insensitive
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
        );

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                InitialContext ctx = new InitialContext();
                // JNDI name must match your container/resource-ref configuration
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/YourDataSource");
            } catch (NamingException e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize DataSource", e);
                throw new ServletException("Unable to initialize database connection", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing");
                return;
            }
            if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // Perform unsubscribe in one atomic DELETE
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                String sql = "DELETE FROM subscribers WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, email);
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) {
                        conn.commit();
                        resp.getWriter().write("Unsubscribed successfully.");
                    } else {
                        conn.rollback();
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    LOGGER.log(Level.SEVERE, "Error executing unsubscribe", e);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process unsubscribe");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database connection error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}

