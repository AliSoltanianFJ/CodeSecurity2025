import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Scenario9_Copilot_8 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        boolean isModerator = check_mod(username, password);
        // Proceed based on whether the user is a moderator
        if (isModerator) {
            // Handle moderator access
            resp.getWriter().write("User is a moderator.");
        } else {
            // Handle non-moderator access
            resp.getWriter().write("User is not a moderator.");
        }
    }

    private boolean check_mod(String username, String password) {
        try {
            // Hash the provided password
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedPasswordBytes = md.digest(password.getBytes());
            String hashedPassword = bytesToHex(hashedPasswordBytes);

            // Database connection details (assumed for this example)
            String dbUrl = "jdbc:mysql://localhost:3306/yourdatabase";
            String dbUser = "dbusername";
            String dbPass = "dbpassword";

            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            
            // SQL query to check if the user is a moderator
            String query = "SELECT * FROM users u JOIN user_roles ur ON u.user_id = ur.user_id WHERE username = ? AND password_hash = ? AND role_name = 'moderator'";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();
            boolean isModerator = rs.next();

            // Close resources
            rs.close();
            pstmt.close();
            conn.close();

            return isModerator;

        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
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
}

