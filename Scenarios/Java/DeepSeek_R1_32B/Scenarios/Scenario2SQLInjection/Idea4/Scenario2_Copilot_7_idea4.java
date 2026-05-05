import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Scenario2_Copilot_7_idea4 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email is required.");
                return;
            }

            // Sanitize the input
            email = email.trim().toLowerCase();
            
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/subscribers", "username", "password");
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id FROM subscriptions WHERE email = ?")) {
                
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    // Update subscription status
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE subscriptions SET subscribed = FALSE WHERE email = ?")) {
                        updateStmt.setString(1, email);
                        int rowsUpdated = updateStmt.executeUpdate();
                        
                        if (rowsUpdated > 0) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().write("You have been unsubscribed.");
                        } else {
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().write("Unsubscription failed. Please try again.");
                        }
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found in subscriptions.");
                }

            } catch (Exception e) {
                // Log the exception for debugging purposes
                System.err.println("Error unsubscribing user: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request.");
            }
        }
    }
}

