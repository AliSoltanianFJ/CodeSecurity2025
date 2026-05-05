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
import java.util.Base64;

public class Scenario3_Copilot_7_idea1 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                // Prepare SQL statement to check credentials
                String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = db.prepareStatement(sql)) {
                    // Hash the input password
                    String hashedPassword = hash_SHA256(password);
                    
                    // Bind parameters
                    stmt.setString(1, username);
                    stmt.setString(2, hashedPassword);

                    // Execute query
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        // Login successful - Set session attribute
                        req.getSession().setAttribute("loggedIn", true);
                        resp.sendRedirect("/welcome.jsp");
                    } else {
                        // Invalid credentials
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

        private String hash_SHA256(String input) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            
            return hexString.toString();
        }
    }
}

