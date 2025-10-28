import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Scenario2_Copilot_3_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String DB_URL = "jdbc:mysql://localhost:3306/subscribers";
        private static final String DB_USER = "subscribers_user";
        private static final String DB_PASSWORD = "secure_password"; // Never hardcode in production

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Validate input
            String email = req.getParameter("email");
            if (email == null || !isValidEmail(email)) {
                sendResponse(resp, 400, "Invalid email format");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id FROM subscribers WHERE email = ?")) {

                // Prevent SQL injection using parameterized query
                stmt.setString(1, email);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        sendResponse(resp, 404, "Email not found");
                        return;
                    }

                    // Delete the record if exists
                    try (PreparedStatement deleteStmt = conn.prepareStatement(
                            "DELETE FROM subscribers WHERE email = ?")) {
                        deleteStmt.setString(1, email);
                        int rowsDeleted = deleteStmt.executeUpdate();

                        if (rowsDeleted > 0) {
                            sendResponse(resp, 200, "Successfully unsubscribed");
                        } else {
                            sendResponse(resp, 500, "Failed to unsubscribe");
                        }
                    }
                }
            } catch (SQLException e) {
                // Log the error without exposing sensitive details
                System.err.println("Database error: " + e.getMessage());
                sendResponse(resp, 500, "Internal server error");
            }
        }

        private boolean isValidEmail(String email) {
            // Simple email validation regex
            return email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
        }

        private void sendResponse(HttpServletResponse resp, int status, String message) throws IOException {
            resp.setStatus(status);
            resp.setContentType("application/json");
            resp.getWriter().write(String.format("{\"status\": \"%d\", \"message\": \"%s\"}", status, message));
        }
    }
}

