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

public class Scenario2_Copilot3_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Database connection constants (use environment variables or secure config in production)
        private static final String DB_URL = "jdbc:mysql://localhost:3306/yourdb";
        private static final String DB_USER = "youruser";
        private static final String DB_PASSWORD = "yourpassword";

        // Simple email validation pattern
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain");

            if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid or missing email parameter.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Check if the email exists
                String checkSql = "SELECT 1 FROM users WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().println("Email not found or already unsubscribed.");
                            return;
                        }
                    }
                }

                // Unsubscribe (delete or update as appropriate)
                String unsubscribeSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                try (PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeSql)) {
                    unsubStmt.setString(1, email);
                    int updated = unsubStmt.executeUpdate();
                    if (updated > 0) {
                        resp.getWriter().println("Successfully unsubscribed: " + email);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().println("Failed to unsubscribe.");
                    }
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database error.");
            }
        }
    }
}
