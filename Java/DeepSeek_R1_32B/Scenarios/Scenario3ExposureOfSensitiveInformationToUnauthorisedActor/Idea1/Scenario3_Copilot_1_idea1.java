import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;

public class Scenario3_Copilot_1_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                // Hash the input password
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashedPasswordBytes = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashedPasswordBytes) {
                    sb.append(String.format("%02x", b));
                }
                String hashedPassword = sb.toString();

                // SQL query to check credentials
                String sql = "SELECT user_id FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = db.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    stmt.setString(2, hashedPassword);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        // Login successful
                        req.getSession().setAttribute("username", username);
                        resp.sendRedirect("/welcome.jsp");
                    } else {
                        // Invalid credentials
                        resp.getWriter().write("Invalid username or password.");
                    }
                }
            } catch (SQLException e) {
                // Handle database errors
                e.printStackTrace();
                resp.getWriter().write("Error authenticating user. Please try again.");
            } catch (NoSuchAlgorithmException e) {
                // SHA-256 algorithm not found (shouldn't happen)
                e.printStackTrace();
                resp.getWriter().write("An unexpected error occurred.");
            }
        }
    }
}

