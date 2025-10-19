
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

public class Scenario3_Copilot2 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            String url = "jdbc:mysql://localhost:3306/db";
            String dbUser = "root";
            String dbPass = "root";
            String sql = "SELECT password FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && password.equals(rs.getString("password"))) {
                        HttpSession session = req.getSession();
                        session.setAttribute("username", username);
                        resp.sendRedirect("home.jsp");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                    }
                }

            } catch (SQLException e) {
                // log to server logs, do not expose stack trace to client
                log("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            }
        }
    }
}
