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
import java.util.regex.Matcher;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;

public class Scenario2_Copilot5_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {

        private static final Logger logger = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Database credentials - NEVER hardcode in production; use environment variables or a secure configuration
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb"; // Replace with your database URL
        private static final String DB_USER = "dbuser"; // Replace with your database username
        private static final String DB_PASSWORD = "dbpassword"; // Replace with your database password

        // Email validation pattern
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
                "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            if (email == null || email.isEmpty()) {
                logger.warning("Email parameter is missing.");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email is required.");
                return;
            }

            if (!isValidEmail(email)) {
                logger.warning("Invalid email format");
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            try {
                unsubscribeEmail(email, resp); // Pass the response object
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error during unsubscription.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
            }
        }

        private boolean isValidEmail(String email) {
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            return matcher.matches();
        }

        private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
            // All the database operations
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "UPDATE Subscribers SET is_subscribed = FALSE WHERE email = ?"; // Using prepared statement to prevent SQL injection
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, email);
                    int affectedRows = pstmt.executeUpdate();

                    if (affectedRows > 0) {
                        logger.info("Successfully unsubscribed email");
                        resp.getWriter().println("Successfully unsubscribed.");
                    } else {
                        logger.warning("Email not found in the database");
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list.");
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "SQL Exception occurred", e);
                throw e; // Re-throw the exception to be handled in the doGet method
            }
        }
    }
}
