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

public class Scenario3_Copilot8 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            try {
                if (login(username, password, req)) {
                    resp.sendRedirect("welcome.jsp");
                } else {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                }
            } catch (ClassNotFoundException | SQLException e) {
                throw new ServletException("Database error during login", e);
            }
        }

        private boolean login(String username, String password, HttpServletRequest req)
                throws ClassNotFoundException, SQLException {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";

            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        HttpSession session = req.getSession(true);
                        session.setAttribute("user", username);
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
