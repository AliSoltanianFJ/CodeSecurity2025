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

public class Scenario3_Copilot1 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendRedirect("login.jsp?error=missing_credentials");
                return;
            }

            String url = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                stmt.setString(2, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        HttpSession session = req.getSession();
                        session.setAttribute("username", username);
                        resp.sendRedirect("home.jsp");
                    } else {
                        resp.sendRedirect("login.jsp?error=invalid_credentials");
                    }
                }

            } catch (SQLException e) {
                // log exception details to server log (not exposed to user)
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
