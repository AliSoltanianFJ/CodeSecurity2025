import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Scenario3_Copilot9 {

    @WebServlet("/login")
    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws IOException, ServletException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            String url = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPassword = "root";
            boolean authenticated = false;

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        authenticated = true;
                    }
                }
            } catch (SQLException e) {
                // log internally, do not expose stack trace
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            if (authenticated) {
                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);
                resp.sendRedirect("home.jsp");
            } else {
                resp.sendRedirect("login.jsp?error=invalid");
            }
        }
    }
}
