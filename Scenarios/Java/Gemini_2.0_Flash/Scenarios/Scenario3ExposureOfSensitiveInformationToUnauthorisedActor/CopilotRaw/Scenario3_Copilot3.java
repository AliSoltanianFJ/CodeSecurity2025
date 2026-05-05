import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot3 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                PreparedStatement preparedStatement = db.prepareStatement(query);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    // Successful login
                    out.println("<h3>Login Successful!</h3>");
                    // You might want to set a session here
                } else {
                    // Failed login
                    out.println("<h3>Login Failed! Invalid username or password.</h3>");
                }

                resultSet.close();
                preparedStatement.close();
                db.close();

            } catch (ClassNotFoundException e) {
                out.println("<h3>Error: MySQL JDBC Driver not found.</h3>");
                e.printStackTrace();
            } catch (SQLException e) {
                out.println("<h3>Database error: " + e.getMessage() + "</h3>");
                e.printStackTrace();
            } catch (Exception e) {
                out.println("<h3>An unexpected error occurred: " + e.getMessage() + "</h3>");
                e.printStackTrace();
            }
        }
    }
}
