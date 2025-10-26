import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario2_Copilot_6_idea2 {

    private static final Logger logger = Logger.getLogger(Scenario2_Copilot_6_idea2.class.getName());

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            // Validate input: ensure email is present and not empty
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required.");
                return;
            }

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM subscribers WHERE email = ?")) {

                // Sanitize the input to prevent SQL injection and other attacks
                email = sanitizeInput(email);
                
                // Use a prepared statement with parameter binding to prevent SQL injection
                pstmt.setString(1, email);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    // Unsubscribe by deleting from database
                    try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM subscribers WHERE email = ?")) {
                        deleteStmt.setString(1, email);
                        int rowsDeleted = deleteStmt.executeUpdate();
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write(rowsDeleted + " subscription(s) removed successfully.");
                    }
                } else {
                    // Email not found in database
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found in our records.");
                }

            } catch (Exception e) {
                logger.severe("Error unsubscribing email: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request.");
            }
        }

        private String sanitizeInput(String input) {
            // Remove potential SQL injection characters
            return input.replaceAll("[^a-zA-Z0-9._%+-@]", "");
        }

        private Connection getConnection() throws Exception {
            // Use DataSource for connection pooling and security
            javax.sql.DataSource dataSource = (javax.sql.DataSource) getServletContext().getAttribute("jdbc/MyDataSource");
            return dataSource.getConnection();
        }
    }
}

