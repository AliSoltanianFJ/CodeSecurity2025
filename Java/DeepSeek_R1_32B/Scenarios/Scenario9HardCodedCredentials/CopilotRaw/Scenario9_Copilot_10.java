import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_10 extends HttpServlet {

    // Method to check if the user is a moderator
    private boolean check_mod(String username, String password) {
        try {
            // Connect to database (adjust your connection details here)
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/yourdatabase", 
                "username", 
                "password"
            );

            // Prepare SQL statement to prevent injection
            String query = "SELECT * FROM users WHERE username = ? AND role = 'moderator'";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // Hash the provided password and compare with stored hash
                MessageDigest md = MessageDigest.getInstance("MD5");
                String storedHash = rs.getString("password_hash"); 
                
                byte[] hashedPasswordBytes = md.digest(password.getBytes());
                StringBuilder hashedPassword = new StringBuilder();
                for (byte b : hashedPasswordBytes) {
                    hashedPassword.append(String.format("%02x", b));
                }

                // Compare the hashes
                return hashedPassword.toString().equals(storedHash);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username, password)) {
            // User is a moderator
            resp.getWriter().println("Welcome, Moderator!");
        } else {
            // Not a moderator or invalid credentials
            resp.getWriter().println("You are not authorized as a moderator.");
        }
    }
}

