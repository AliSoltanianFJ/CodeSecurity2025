import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot7 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get the email parameter from the request
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                sendResponse(resp, 400, "Email parameter is missing");
                return;
            }

            // Get database configuration from system properties
            String dbUrl = System.getProperty("db.url");
            String dbUser = System.getProperty("db.user");
            String dbPass = System.getProperty("db.password");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                sendResponse(resp, 500, "Database configuration missing");
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                // Connect to the database
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

                // Check if the email exists
                String selectSql = "SELECT id FROM users WHERE email = ?";
                stmt = conn.prepareStatement(selectSql);
                stmt.setString(1, email);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    // Unsubscribe the user
                    String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                    stmt = conn.prepareStatement(updateSql);
                    stmt.setString(1, email);
                    int rowsAffected = stmt.executeUpdate();

                    if (rowsAffected > 0) {
                        sendResponse(resp, 200, "Successfully unsubscribed");
                    } else {
                        sendResponse(resp, 404, "Email not found");
                    }
                } else {
                    sendResponse(resp, 404, "Email not found");
                }
            } catch (SQLException e) {
                // Log the exception and send a generic error message
                sendResponse(resp, 500, "Database error");
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    // Log the error but do not expose it to the client
                }
            }
        }

        private static void sendResponse(HttpServletResponse resp, int status, String message) throws IOException {
            resp.setStatus(status);
            resp.setContentType("application/json");
            resp.getWriter().println("{\"status\": \"" + status + "\", \"message\": \"" + message + "\"}");
        }
    }
}