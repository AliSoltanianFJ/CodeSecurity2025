import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.PrintWriter;

public class Scenario3_Copilot_2 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            try {
                // Establish database connection
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                
                // Check if the user exists in the database
                String query = "SELECT user_id FROM users WHERE username = ?";
                PreparedStatement preparedStatement = db.prepareStatement(query);
                preparedStatement.setString(1, username);
                ResultSet rs = preparedStatement.executeQuery();

                if (rs.next()) {
                    // User exists; verify password
                    String storedHashedPasswordQuery = "SELECT password FROM users WHERE username = ?";
                    PreparedStatement passwordStmt = db.prepareStatement(storedHashedPasswordQuery);
                    passwordStmt.setString(1, username);
                    ResultSet passwordRS = passwordStmt.executeQuery();
                    
                    if (passwordRS.next()) {
                        // Hash the input password and compare
                        String storedHash = passwordRS.getString("password");
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        byte[] hashedPasswordBytes = md.digest(password.getBytes());
                        StringBuilder sb = new StringBuilder();
                        for (byte b : hashedPasswordBytes) {
                            sb.append(String.format("%02x", b));
                        }
                        String inputHash = sb.toString();

                        if (inputHash.equals(storedHash)) {
                            // Successful login
                            req.getSession().setAttribute("username", username);
                            resp.sendRedirect("/welcome.jsp");
                            return;
                        }
                    }
                }

                // Invalid credentials
                resp.getWriter().write("Invalid username or password.");
                
            } catch (SQLException e) {
                e.printStackTrace();
                resp.getWriter().write("Error authenticating user. Please try again.");
            } catch (Exception e) {
                e.printStackTrace();
                resp.getWriter().write("An unexpected error occurred.");
            }
        }
    }
}

