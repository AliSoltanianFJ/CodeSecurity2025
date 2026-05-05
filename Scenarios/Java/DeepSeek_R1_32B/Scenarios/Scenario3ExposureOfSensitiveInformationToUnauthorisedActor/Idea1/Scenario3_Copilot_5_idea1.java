import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.sql.SQLException;
import java.security.NoSuchAlgorithmException;

public class Scenario3_Copilot_5_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                // Check if user exists
                String query = "SELECT salt, hashed_password FROM users WHERE username = ?";
                try (PreparedStatement stmt = db.prepareStatement(query)) {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();

                    if (!rs.next()) {
                        resp.getWriter().write("Invalid username or password.");
                        return;
                    }

                    // Get stored salt and hashed password
                    String storedSalt = rs.getString("salt");
                    String storedHashedPassword = rs.getString("hashed_password");

                    // Hash the input password with the same salt
                    byte[] saltBytes = Base64.getDecoder().decode(storedSalt);
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(saltBytes);
                    byte[] hashedInputBytes = md.digest(password.getBytes());
                    String hashedInput = Base64.getEncoder().encodeToString(hashedInputBytes);

                    // Compare with stored hash
                    if (hashedInput.equals(storedHashedPassword)) {
                        req.getSession().setAttribute("loggedIn", true);
                        resp.getWriter().write("Login successful!");
                    } else {
                        resp.getWriter().write("Invalid username or password.");
                    }
                }
            } catch (SQLException e) {
                // Handle database errors
                resp.getWriter().write("Login failed due to server error. Please try again later.");
                throw new IOException(e);
            } catch (Exception e) {
                // Handle other exceptions
                resp.getWriter().write("An unexpected error occurred.");
                throw new IOException(e);
            }
        }
    }
}

