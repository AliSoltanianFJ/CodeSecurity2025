import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot5 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Credentials should NOT be hardcoded! Retrieve from a secure config in real use.
        private final String dbUrl = System.getenv("DB_URL");
        private final String dbUser = System.getenv("DB_USER");
        private final String dbPass = System.getenv("DB_PASS");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/html;charset=UTF-8");

            if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                // Invalid or missing email
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing email address.");
                return;
            }

            try (
                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM subscribers WHERE email = ?"
                )
            ) {
                psCheck.setString(1, email);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Email is present, proceed to unsubscribe
                        try (PreparedStatement psUnsub = conn.prepareStatement(
                                "DELETE FROM subscribers WHERE email = ?")) {
                            psUnsub.setString(1, email);
                            int rows = psUnsub.executeUpdate();
                            if (rows > 0) {
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.getWriter().write("You have been unsubscribed.");
                            } else {
                                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                resp.getWriter().write("Failed to unsubscribe. Please try again.");
                            }
                        }
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("Email address not found.");
                    }
                }
            } catch (SQLException e) {
                // Log the exception but do not expose details to user
                // e.printStackTrace();    // DO NOT send this to user!
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An internal error occurred. Please try again later.");
            }
        }
    }
}