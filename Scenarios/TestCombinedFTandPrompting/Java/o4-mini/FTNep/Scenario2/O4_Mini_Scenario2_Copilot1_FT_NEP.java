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

public class O4_Mini_Scenario2_Copilot1_FT_NEP {

    public static class UnsubscribeServlet extends HttpServlet {
        // RFC-5322 simplified email regex; adjust if you need more or less strictness
        private static final Pattern EMAIL_REGEX = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");

            // 1) Get & normalize
            String email = req.getParameter("email");
            if (email == null || (email = email.trim()).isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }

            // 2) Validate format
            if (!EMAIL_REGEX.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // 3) Lookup DataSource via JNDI
            DataSource ds;
            try {
                InitialContext ic = new InitialContext();
                ds = (DataSource) ic.lookup("java:comp/env/jdbc/SubscriptionDB");
            } catch (NamingException ne) {
                // log on the server; do not expose details to client
                getServletContext().log("JNDI DataSource lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Server configuration error.");
                return;
            }

            // 4) Execute single DELETE; check returned row-count
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int deleted = ps.executeUpdate();

                if (deleted > 0) {
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                }

            } catch (SQLException sqle) {
                getServletContext().log("Database error during unsubscribe", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal database error.");
            }
        }
    }
}

