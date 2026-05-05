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

public class Scenario3_Copilot4 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            Connection db = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                stmt = db.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
                stmt.setString(1, username);
                stmt.setString(2, password);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    // Login successful
                    resp.getWriter().write("Login successful!");
                } else {
                    // Login failed
                    resp.getWriter().write("Invalid username or password.");
                }
            } catch (SQLException e) {
                resp.getWriter().write("Database error occurred.");
                // Optionally log the exception
                // e.printStackTrace();
            } finally {
                try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
                try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
                try { if (db != null) db.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
