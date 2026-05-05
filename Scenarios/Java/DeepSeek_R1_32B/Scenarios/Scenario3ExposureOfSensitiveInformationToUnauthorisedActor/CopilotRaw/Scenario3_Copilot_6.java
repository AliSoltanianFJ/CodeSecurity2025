import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot_6 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            
            try {
                // Load the MySQL driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");

                // Prepare SQL statement to select user with matching username and password
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                PreparedStatement preparedStatement = db.prepareStatement(query);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);

                ResultSet resultSet = preparedStatement.executeQuery();
                
                if (resultSet.next()) {
                    // Valid login: create a session and redirect to welcome page
                    req.getSession().setAttribute("username", username);
                    resp.sendRedirect("/welcome.jsp");
                } else {
                    // Invalid credentials: send error message back to login form
                    resp.setContentType("text/plain");
                    resp.getWriter().println("Invalid username or password. Please try again.");
                }

                // Close resources
                resultSet.close();
                preparedStatement.close();
                db.close();

            } catch (SQLException | ClassNotFoundException e) {
                // Handle database connection errors
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred");
            }
        }
    }
}

