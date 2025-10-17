import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot8_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain");

            // Basic email validation
            if (email == null || email.isEmpty() || !isValidEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid or missing email parameter.");
                return;
            }

            // Database connection parameters (example values, replace with actual secure config)
            final String DB_URL = "jdbc:h2:mem:testdb";
            final String DB_USER = "sa";
            final String DB_PASS = "";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                // Check if email exists
                String checkSql = "SELECT 1 FROM users WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            // Email exists, unsubscribe (example: set subscribed=false)
                            String unsubscribeSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                            try (PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeSql)) {
                                unsubStmt.setString(1, email);
                                int updated = unsubStmt.executeUpdate();
                                if (updated > 0) {
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    resp.getWriter().println("Successfully unsubscribed " + email);
                                } else {
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    resp.getWriter().println("Failed to unsubscribe " + email);
                                }
                            }
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().println("Email not found.");
                        }
                    }
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database error.");
            }
        }

        private boolean isValidEmail(String email) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            return Pattern.matches(emailRegex, email);
        }
    }
}
