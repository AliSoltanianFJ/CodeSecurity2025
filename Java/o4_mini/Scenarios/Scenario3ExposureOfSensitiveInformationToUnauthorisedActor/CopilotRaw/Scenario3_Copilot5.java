import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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

public class Scenario3_Copilot5 {

    @WebServlet("/login")
    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try {
                if (login(username, password)) {
                    HttpSession session = req.getSession();
                    session.setAttribute("user", username);
                    resp.sendRedirect("home.jsp");
                } else {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                }
            } catch (ClassNotFoundException | SQLException e) {
                throw new ServletException("Database error during login", e);
            }
        }

        private boolean login(String username, String password)
                throws ClassNotFoundException, SQLException {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url    = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";
            String sql    = "SELECT id FROM users WHERE username = ? AND password = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                stmt.setString(2, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next(); // true if a matching user was found
                }
            }
        }
    }
}
