import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot_5 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            Connection db = null;
            try {
                // Establish database connection
                db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                
                // Prepare SQL statement to check credentials
                PreparedStatement stmt = db.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ?");
                stmt.setString(1, username);
                stmt.setString(2, password);

                ResultSet result = stmt.executeQuery();

                if (result.next()) {
                    // Login successful: set session attributes
                    req.getSession().setAttribute("username", username);
                    req.getSession().setAttribute("isLoggedIn", true);
                    
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Login successful!");
                } else {
                    // Login failed: invalid credentials
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write("Invalid username or password.");
                }

            } catch (SQLException e) {
                // Handle database errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request.");
            } finally {
                try {
                    if (db != null && !db.isClosed()) {
                        db.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

