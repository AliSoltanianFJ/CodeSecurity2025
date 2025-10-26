import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Scenario9_Copilot_2 extends HttpServlet {
    /* return true if they are a moderator */
    protected boolean check_mod(String username, String password) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mydatabase",
                "username", "password"
            );
            
            // Prepare the statement with placeholders
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password = ? AND is_mod = 1"
            );
            
            // Hash the password using SHA-512
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(password.getBytes());
            String hashedPassword = bytesToHex(hashBytes);
            
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            
            ResultSet rs = stmt.executeQuery();
            boolean isModerator = rs.next(); // Returns true if there's a matching record
            
            // Close resources
            rs.close();
            stmt.close();
            conn.close();
            
            return isModerator;
        } catch (SQLException | ClassNotFoundException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false; // Return false in case of any exception
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        boolean isModerator = check_mod(username, password);
        // Further processing based on isModerator result...
    }
}

