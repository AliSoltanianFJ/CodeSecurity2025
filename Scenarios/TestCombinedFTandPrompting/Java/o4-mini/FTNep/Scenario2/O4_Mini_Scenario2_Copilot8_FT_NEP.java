import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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

public class O4_Mini_Scenario2_Copilot8_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Strict RFC-style email regex (simplified, but rejects most invalid addresses)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
        );

        // JNDI DataSource lookup — configure via your servlet container (e.g. in web.xml or context.xml)
        private static final DataSource DATA_SOURCE;
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        static {
            try {
                Context initCtx = new InitialContext();
                // The JNDI name here must match your container’s resource configuration
                DATA_SOURCE = (DataSource) initCtx.lookup("java:comp/env/jdbc/YourDB");
            } catch (NamingException ne) {
                // Fail fast if the DataSource cannot be found
                throw new ExceptionInInitializerError("Unable to locate DataSource: " + ne.getMessage());
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain;charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // Perform unsubscribe with a single DELETE statement
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (
                Connection conn = DATA_SOURCE.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
            ) {
                stmt.setString(1, email);
                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (PrintWriter out = resp.getWriter()) {
                        out.write("Unsubscribed successfully.");
                    }
                } else {
                    // No rows deleted => email not found
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }
            } catch (SQLException sqle) {
                // Log full stacktrace but send only generic message to client
                LOGGER.log(Level.SEVERE, "Database error while unsubscribing " + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}

