import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot1_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        // TODO: move these into configuration or a connection‐pool‐backed DataSource
        private static final String JDBC_URL  = "jdbc:mysql://localhost:3306/yourdb?useSSL=true&serverTimezone=UTC";
        private static final String JDBC_USER = "youruser";
        private static final String JDBC_PASS = "yourpass";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            // 1) Get parameter and basic validation
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty() || !isValidEmail(email.trim())) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid or missing email parameter.");
                return;
            }
            email = email.trim().toLowerCase();

            // 2) Talk to the database
            try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {

                // 2a) Check existence
                String selectSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        int count = rs.getInt(1);
                        if (count == 0) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().println("Email address not found in our list.");
                            return;
                        }
                    }
                }

                // 2b) Perform unsubscribe (flip a flag)
                String updateSql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, email);
                    int rowsUpdated = ps.executeUpdate();
                    if (rowsUpdated > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().println("You have been unsubscribed successfully.");
                    } else {
                        // This should not normally happen if the SELECT above said it existed
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("Unable to unsubscribe at this time.");
                    }
                }

            } catch (SQLException sqle) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database error: " + sqle.getMessage());
                // In production, log the exception rather than echoing it
            }
        }

        /**
         * Very simple RFC-5322-lite check. For real production use,
         * consider using a well-tested library or stricter regex.
         */
        private static boolean isValidEmail(String email) {
            // letters, digits, plus, underscore, dot or hyphen before the @
            // then letters, digits, hyphens or dots after.
            return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        }
    }
}
