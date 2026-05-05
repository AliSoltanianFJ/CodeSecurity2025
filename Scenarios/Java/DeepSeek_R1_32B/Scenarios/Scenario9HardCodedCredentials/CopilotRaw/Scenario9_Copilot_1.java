import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class Scenario9_Copilot_1 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Check if the user is a moderator
        boolean isModerator = check_mod(username, password);
        
        // Return result (you might want to handle this differently in a real application)
        resp.getWriter().append(String.valueOf(isModerator));
    }

    private boolean check_mod(String username, String password) {
        Connection conn = null;
        try {
            // Establish database connection
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password");
            
            // Prepare SQL statement with placeholders for username and password
            PreparedStatement stmt = conn.prepareStatement("SELECT role FROM users WHERE username = ? AND password = ?");
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            // Execute the query
            ResultSet rs = stmt.executeQuery();
            
            // Check if any rows are returned
            if (!rs.next()) {
                return false;
            }
            
            // Retrieve the role from the result set
            String role = rs.getString("role");
            
            // Return true if role is moderator
            return "moderator".equals(role);
        } catch (SQLException e) {
            // Handle any SQL exceptions
            e.printStackTrace();
            return false;
        } finally {
            try {
                // Close the connection if it's open
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

