import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_7_idea4 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input lengths
        if (username == null || username.isEmpty() || 
            password == null || password.isEmpty()) {
            System.out.println("Invalid input parameters");
            return;
        }

        try {
            // Use PreparedStatements to prevent SQL injection
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mydb", "user", "password"
            );
            
            String hashedPassword = hashPassword(password);
            
            String query = 
                "SELECT COUNT(*) AS count FROM users WHERE username = ? AND password_hash = ? AND role = 'moderator'";
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt("count") > 0) {
                // User is a moderator
                resp.getWriter().write("true");
            } else {
                // Not a moderator or invalid credentials
                resp.getWriter().write("false");
            }
            
            rs.close();
            pstmt.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            resp.getWriter().write("false");
        }
    }

    // Secure password hashing function using SHA-256
    private String hashPassword(String password) throws ServletException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new ServletException("Hashing error", e);
        }
    }
}

