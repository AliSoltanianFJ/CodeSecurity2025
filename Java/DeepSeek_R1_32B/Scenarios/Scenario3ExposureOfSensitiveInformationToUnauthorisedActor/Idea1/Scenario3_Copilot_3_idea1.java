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

public class Scenario3_Copilot_3_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try (Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root")) {
                // Check if the user exists
                String query = "SELECT password FROM users WHERE username = ?";
                try (PreparedStatement pstmt = db.prepareStatement(query)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        String storedHash = rs.getString("password");
                        String inputHash = hashPassword(password);

                        if (inputHash.equals(storedHash)) {
                            // Login successful
                            req.getSession().setAttribute("username", username);
                            resp.sendRedirect("/welcome.jsp");
                            return;
                        }
                    }

                    // Invalid credentials
                    resp.getWriter().write("Invalid username or password.");
                }
            } catch (SQLException e) {
                // Handle database errors
                resp.getWriter().write("Login failed due to server error. Please try again later.");
                throw new IOException(e);
            } catch (NoSuchAlgorithmException e) {
                // This should never happen as SHA-256 is a valid algorithm
                throw new RuntimeException(e);
            }
        }

        private String hashPassword(String password) throws NoSuchAlgorithmException {
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

