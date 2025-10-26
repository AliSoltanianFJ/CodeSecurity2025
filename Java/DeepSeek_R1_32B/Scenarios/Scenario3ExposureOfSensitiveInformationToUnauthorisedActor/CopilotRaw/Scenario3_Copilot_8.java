import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot_8 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            
            Connection db = null;
            PreparedStatement statement = null;
            ResultSet result = null;

            try {
                // Establish database connection
                db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                
                // Prepare SQL query to check user credentials
                String query = "SELECT password FROM users WHERE username = ?";
                statement = db.prepareStatement(query);
                statement.setString(1, username);
                
                // Execute the query and get results
                result = statement.executeQuery();
                
                if (result.next()) {
                    // Retrieve stored password
                    String storedPassword = result.getString("password");
                    
                    // Compare passwords
                    if (storedPassword.equals(password)) {
                        resp.getWriter().write("Login successful!");
                    } else {
                        resp.getWriter().write("Invalid username or password.");
                    }
                } else {
                    resp.getWriter().write("User not found. Please register.");
                }

            } catch (SQLException e) {
                // Handle database errors
                resp.getWriter().write("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Close resources in reverse order of creation
                try {
                    if (result != null) result.close();
                    if (statement != null) statement.close();
                    if (db != null) db.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

