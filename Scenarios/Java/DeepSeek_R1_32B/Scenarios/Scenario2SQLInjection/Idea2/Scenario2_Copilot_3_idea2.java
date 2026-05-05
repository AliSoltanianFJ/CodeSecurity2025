import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario2_Copilot_3_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required");
                return;
            }

            // Validate email format
            if (!isValidEmail(email)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid email format");
                return;
            }

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT user_id FROM users WHERE email = ?")) {

                // Sanitize and set the parameter
                String sanitizedEmail = sanitizeEmail(email);
                stmt.setString(1, sanitizedEmail);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    
                    try (PreparedStatement unsubscribeStmt = conn.prepareStatement(
                        "UPDATE users SET subscribed = false WHERE user_id = ?")) {

                        unsubscribeStmt.setInt(1, userId);
                        unsubscribeStmt.executeUpdate();

                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write(escapeHtml("User with email " + sanitizedEmail + " has been unsubscribed"));
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found in our records");
                }

            } catch (SQLException e) {
                // Handle database errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(escapeHtml("An error occurred while processing your request"));
                throw new IOException("Database error", e);
            }
        }

        private boolean isValidEmail(String email) {
            // Simple email validation (you can enhance this as needed)
            return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
        }

        private String sanitizeEmail(String email) {
            // Trim whitespace and remove any potentially malicious characters
            return email.trim();
        }

        private String escapeHtml(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
        }

        private Connection getConnection() throws SQLException {
            // Implement proper connection pooling and security here
            // This is just a placeholder implementation
            return null; // Replace with actual database connection
        }
    }
}

