import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot_3 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            
            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                // SQL injection-safe query
                String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = db.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        // Login successful
                        resp.getWriter().println("Login successful!");
                    } else {
                        // Invalid credentials
                        resp.getWriter().println("Invalid username or password.");
                    }
                }
            } catch (SQLException e) {
                // Handle database errors
                e.printStackTrace();
                resp.getWriter().println("An error occurred while logging in.");
            }
        }
    }
}

