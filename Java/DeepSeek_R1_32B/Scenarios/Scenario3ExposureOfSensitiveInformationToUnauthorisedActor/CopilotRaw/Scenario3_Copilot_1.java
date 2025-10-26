import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.PrintWriter;

public class Scenario3_Copilot_1 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            
            Connection dbConnection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;

            try {
                // Establish database connection
                dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                
                // Prepare SQL statement to check if username exists
                String query = "SELECT password FROM users WHERE username = ?";
                preparedStatement = dbConnection.prepareStatement(query);
                preparedStatement.setString(1, username);
                
                // Execute the query
                resultSet = preparedStatement.executeQuery();
                
                // Check if username exists
                if (resultSet.next()) {
                    // Get stored password
                    String storedPassword = resultSet.getString("password");
                    
                    // Compare passwords
                    if (password.equals(storedPassword)) {
                        // Login successful, set session attribute
                        req.getSession().setAttribute("loggedIn", true);
                        resp.getWriter().write("Login successful!");
                    } else {
                        // Incorrect password
                        resp.getWriter().write("Invalid username or password.");
                    }
                } else {
                    // Username not found
                    resp.getWriter().write("Invalid username or password.");
                }
            } catch (SQLException e) {
                // Handle any database errors
                resp.getWriter().write("Login failed due to server error. Please try again later.");
                throw new IOException(e);
            } finally {
                // Close resources to avoid leaks
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (dbConnection != null) dbConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

