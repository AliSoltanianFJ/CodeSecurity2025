// ...existing code...
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
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
import java.util.regex.Pattern;

public class Scenario2_Copilot9_idea2 {

    @WebServlet("/unsubscribe")
    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Very conservative email pattern: limits length and basic structure.
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9._%+\\-]{1,64}@[A-Za-z0-9.\\-]{1,253}\\.[A-Za-z]{2,63}$"
        );

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Missing email parameter.");
                }
                return;
            }

            email = email.trim();
            if (email.length() == 0 || email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Invalid email address.");
                }
                return;
            }

            // Do not reflect the provided email back in the response to avoid data exposure / XSS.
            // Use a DataSource via JNDI. Configure "java:comp/env/jdbc/MyDB" in your container.
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");

                // Single transaction: check if present and, if so, mark unsubscribed.
                try (Connection conn = ds.getConnection()) {
                    conn.setAutoCommit(false);
                    boolean existedAndUnsubscribed = false;

                    String selectSql = "SELECT subscribed FROM subscribers WHERE email = ? FOR UPDATE";
                    try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                        psSelect.setString(1, email);
                        try (ResultSet rs = psSelect.executeQuery()) {
                            if (rs.next()) {
                                boolean subscribed = rs.getBoolean("subscribed");
                                if (subscribed) {
                                    String updateSql = "UPDATE subscribers SET subscribed = FALSE, updated_at = CURRENT_TIMESTAMP WHERE email = ?";
                                    try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                                        psUpdate.setString(1, email);
                                        int updated = psUpdate.executeUpdate();
                                        if (updated > 0) {
                                            existedAndUnsubscribed = true;
                                        }
                                    }
                                } else {
                                    // Already unsubscribed: treat as successful but no sensitive info revealed.
                                    existedAndUnsubscribed = true;
                                }
                            }
                        }
                    }

                    conn.commit();

                    // Generic response to avoid account enumeration.
                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter w = resp.getWriter()) {
                        w.println("If the provided address was registered, the unsubscribe request has been processed.");
                        if (!existedAndUnsubscribed) {
                            // Do not reveal that the email was not found; keep generic.
                        }
                    }
                }
            } catch (NamingException | SQLException e) {
                // Do not expose internal error details or sensitive information to the client.
                // Log minimally if a logging system is available; here we defer to servlet container logging.
                getServletContext().log("Unsubscribe operation failed (internal).");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("An internal error occurred while processing the request.");
                }
            }
        }
    }
}
// ...existing code...
