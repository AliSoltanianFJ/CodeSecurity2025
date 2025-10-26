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

public class Scenario3_Copilot_4 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/db", "root", "root")) {

                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                PreparedStatement statement = db.prepareStatement(query);
                statement.setString(1, username);
                statement.setString(2, password);

                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    // Login successful
                    req.getSession().setAttribute("username", username);
                    resp.sendRedirect("/welcome.jsp");
                } else {
                    // Login failed
                    resp.getWriter().println("Invalid username or password");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                resp.getWriter().println("Error logging in: " + e.getMessage());
            }
        }
    }
}

