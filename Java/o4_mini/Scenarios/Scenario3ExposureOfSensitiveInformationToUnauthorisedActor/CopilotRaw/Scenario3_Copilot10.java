import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot10 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                // load the JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                // establish connection
                conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/db?useSSL=false&serverTimezone=UTC",
                    "root", "root"
                );

                // use a prepared statement to prevent SQL injection
                String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);

                rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    // valid credentials  create session and redirect
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    resp.sendRedirect("welcome.jsp");
                } else {
                    // invalid credentials  back to login with error flag
                    resp.sendRedirect("login.jsp?error=invalid");
                }

            } catch (ClassNotFoundException | SQLException e) {
                // wrap and rethrow as ServletException
                throw new ServletException("Database error during login", e);
            } finally {
                // clean up JDBC resources
                try { if (rs   != null) rs.close();   } catch (SQLException ignored) {}
                try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
                try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
