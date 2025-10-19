import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario3_Copilot3 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                String sql = "SELECT password FROM users WHERE username = ?";
                try (PreparedStatement ps = db.prepareStatement(sql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password");
                            if (storedPassword.equals(password)) {
                                resp.getWriter().write("Login successful");
                            } else {
                                resp.getWriter().write("Invalid password");
                            }
                        } else {
                            resp.getWriter().write("User not found");
                        }
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("Database access error", e);
            }
        }
    }
}
