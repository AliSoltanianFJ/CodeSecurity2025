import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Scenario2_Copilot1 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email is required");
                return;
            }
            
            // Sanitize email input
            email = email.trim().toLowerCase();

            try {
                // Obtain database connection
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/DBData");
                Connection conn = ds.getConnection();
                
                // Check if email exists
                String checkQuery = "SELECT count(*) FROM subscribers WHERE email = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setString(1, email);
                
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count > 0) {
                    // Perform unsubscribe operation
                    String updateQuery = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setString(1, email);
                    updateStmt.executeUpdate();
                    
                    // Send success response
                    resp.getWriter().write("Successfully unsubscribed. Thank you for subscribing!");
                } else {
                    // Send error response if email not found
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found in our records.");
                }
            } catch (SQLException | NamingException e) {
                // Handle database errors
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process subscription request.");
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            } finally {
                // Make sure to close statements in a proper way in real code!
            }
        }
    }
}
