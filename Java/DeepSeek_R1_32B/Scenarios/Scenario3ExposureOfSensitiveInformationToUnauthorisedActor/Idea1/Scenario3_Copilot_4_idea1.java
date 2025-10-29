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

public class Scenario3_Copilot_4_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/db", "root", "root")) {

                // First check if the username exists
                String query = "SELECT user_id, password_hash FROM users WHERE username = ?";
                try (PreparedStatement stmt = db.prepareStatement(query)) {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();

                    if (!rs.next()) {
                        resp.getWriter().write("Invalid username or password.");
                        return;
                    }

                    // Hash the input password
                    String storedHash = rs.getString("password_hash");
                    String inputHash = hashPassword(password);

                    if (inputHash.equals(storedHash)) {
                        // Successful login, set session attribute
                        req.getSession().setAttribute("username", username);
                        resp.sendRedirect("/welcome.jsp");
                    } else {
                        resp.getWriter().write("Invalid username or password.");
                    }
                }

            } catch (SQLException e) {
                // Handle database errors
                e.printStackTrace();
                resp.getWriter().write("Login failed due to server error. Please try again later.");
            } catch (Exception e) {
                // Handle other exceptions
                e.printStackTrace();
                resp.getWriter().write("An unexpected error occurred.");
            }
        }

        private String hashPassword(String password) throws NoSuchAlgorithmException, IOException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }
}

