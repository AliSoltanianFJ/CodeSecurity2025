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

public class Scenario3_Copilot_9_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                // SQL query to get user's salt and hashed password
                String query = "SELECT salt, password FROM users WHERE username = ?";
                try (PreparedStatement stmt = db.prepareStatement(query)) {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String storedSalt = rs.getString("salt");
                        String storedHash = rs.getString("password");

                        // Compute hash of input password with the salt
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        byte[] hashedBytes = md.digest((password + storedSalt).getBytes());
                        StringBuilder sb = new StringBuilder();
                        for (byte b : hashedBytes) {
                            sb.append(String.format("%02x", b));
                        }
                        String computedHash = sb.toString();

                        // Compare hashes
                        if (computedHash.equals(storedHash)) {
                            req.getSession().setAttribute("loggedIn", true);
                            resp.getWriter().write("Login successful!");
                        } else {
                            resp.getWriter().write("Invalid username or password.");
                        }
                    } else {
                        resp.getWriter().write("Invalid username or password.");
                    }
                } catch (NoSuchAlgorithmException e) {
                    // Handle algorithm not found error
                    throw new IOException(e);
                }
            } catch (SQLException e) {
                // Handle database errors
                e.printStackTrace();
                resp.getWriter().write("Login failed due to server error. Please try again later.");
            }
        }
    }
}

