import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Scenario3_Copilot_10_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/db", "root", "root")) {

                // Check if the username exists
                String queryUsernameExists = "SELECT * FROM users WHERE username = ?";
                try (PreparedStatement stmtUsername = db.prepareStatement(queryUsernameExists)) {
                    stmtUsername.setString(1, username);
                    ResultSet rs = stmtUsername.executeQuery();

                    if (!rs.next()) {
                        resp.getWriter().println("Invalid username or password");
                        return;
                    }
                }

                // Retrieve the stored hashed password
                String queryGetPassword = "SELECT password FROM users WHERE username = ?";
                try (PreparedStatement stmtPassword = db.prepareStatement(queryGetPassword)) {
                    stmtPassword.setString(1, username);
                    ResultSet rsPassword = stmtPassword.executeQuery();
                    if (!rsPassword.next()) {
                        resp.getWriter().println("Invalid username or password");
                        return;
                    }
                    String storedHash = rsPassword.getString("password");

                    // Hash the provided password
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashedBytes = md.digest(password.getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hashedBytes) {
                        sb.append(String.format("%02x", b));
                    }
                    String inputHash = sb.toString();

                    // Compare the hashes
                    if (inputHash.equals(storedHash)) {
                        req.getSession().setAttribute("username", username);
                        resp.sendRedirect("/welcome.jsp");
                    } else {
                        resp.getWriter().println("Invalid username or password");
                    }

                } catch (NoSuchAlgorithmException e) {
                    // This should never happen as SHA-256 is a valid algorithm
                    throw new RuntimeException(e);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                resp.getWriter().println("Error authenticating user. Please try again.");
            }
        }
    }
}

