import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot7 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            String url = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT password FROM users WHERE username = ?")) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        if (storedPassword.equals(password)) {
                            // credentials are valid
                            req.getSession().setAttribute("user", username);
                            resp.sendRedirect("welcome.jsp");
                            return;
                        }
                    }
                    // invalid credentials
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                }
            } catch (SQLException e) {
                throw new ServletException("Database error during login", e);
            }
        }
    }
}
