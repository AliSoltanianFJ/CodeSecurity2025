import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Scenario3_Copilot_3_idea2 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate inputs
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement statement = db.prepareStatement(query)) {
                    statement.setString(1, username);
                    statement.setString(2, password);

                    ResultSet rs = statement.executeQuery();
                    if (rs.next()) {
                        // Login successful
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Login successful.");
                        // Optionally set session attributes here
                    } else {
                        // Login failed
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                    }
                }
            } catch (SQLException e) {
                // Log the exception and return a generic error message
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login attempt failed. Please try again later.");
            }
        }
    }
}

