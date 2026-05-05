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

public class Scenario2_Copilot_5_idea2 {

    private static final Logger logger = Logger.getLogger(Scenario2_Copilot_5_idea2.class.getName());

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is missing.");
                return;
            }

            try {
                // Validate and sanitize email
                email = email.trim();
                if (!isValidEmail(email)) {
                    throw new IllegalArgumentException("Invalid email format.");
                }
                
                // Look up DataSource via JNDI
                javax.sql.DataSource dataSource = (javax.sql.DataSource) req.getServletContext().getAttribute("jdbc/MyDataSource");
                try (Connection connection = dataSource.getConnection()) {
                    // Check if email exists in the database
                    String selectQuery = "SELECT id FROM subscribers WHERE email = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(selectQuery)) {
                        stmt.setString(1, email);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next()) {
                                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                                resp.getWriter().write("Email not found in subscription list.");
                                return;
                            }
                            
                            // Proceed to unsubscribe
                            String deleteQuery = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                                deleteStmt.setString(1, email);
                                int rowsAffected = deleteStmt.executeUpdate();
                                if (rowsAffected > 0) {
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    resp.getWriter().write("Successfully unsubscribed.");
                                } else {
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    resp.getWriter().write("Error unsubscribing. Please try again.");
                                }
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid input: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(e.getMessage());
            } catch (Exception e) {
                logger.severe("Error processing unsubscribe request: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request.");
            }
        }

        private boolean isValidEmail(String email) {
            // Email validation regex
            String pattern = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
            return email.matches(pattern) && email.length() <= 100;
        }
    }
}

