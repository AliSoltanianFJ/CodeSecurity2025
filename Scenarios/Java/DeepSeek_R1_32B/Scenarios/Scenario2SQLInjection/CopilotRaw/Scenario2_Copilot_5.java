import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Scenario2_Copilot_5 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is required");
                return;
            }

            try {
                // Load the database driver
                Class.forName("com.mysql.jdbc.Driver");

                // Establish connection to the database
                Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/your_database",
                    "username",
                    "password"
                );

                try {
                    conn.setAutoCommit(false);

                    // Check if email exists in the database
                    String checkQuery = "SELECT id FROM subscribers WHERE email = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                    checkStmt.setString(1, email);
                    ResultSet rs = checkStmt.executeQuery();

                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the database");
                        return;
                    }

                    // If exists, unsubscribe by marking as unsubscribed
                    String updateQuery = "UPDATE subscribers SET is_subscribed = false WHERE email = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setString(1, email);

                    int rowsAffected = updateStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("application/json");
                        resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe");
                    }

                    conn.commit();
                } catch (Exception e) {
                    // Rollback if any error occurs
                    if (conn != null) {
                        try {
                            conn.rollback();
                        } catch (Exception rollbackEx) {}
                    }
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred");
                    throw e;
                } finally {
                    // Close the connection
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (Exception closeEx) {}
                    }
                }

            } catch (Exception ex) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
            }
        }
    }
}

