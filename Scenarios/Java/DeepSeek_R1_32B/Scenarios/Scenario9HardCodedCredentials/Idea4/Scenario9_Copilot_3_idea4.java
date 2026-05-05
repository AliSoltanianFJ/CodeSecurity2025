import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Scenario9_Copilot_3_idea4 extends HttpServlet {
    private final String DB_URL = "jdbc:mysql://localhost:3306/yourDatabase";
    private final String DB_USER = "username";
    private final String DB_PASSWORD = "password";

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (checkMod(username, password)) {
            // Handle moderator access
            resp.getWriter().write("Moderator access granted.");
        } else {
            // Handle non-moderator access
            resp.getWriter().write("Access denied.");
        }
    }

    private boolean checkMod(String username, String password) {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password = ?")) {

            // Hash the input password
            String hashedPassword = hashMD5(password);

               pstmt.setString(1, username);
               pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returns true if there is a matching moderator

        } catch (SQLException e) {
            // Log the error securely without exposing sensitive information
            System.err.println("Database error occurred: " + e.getMessage());
            return false;
        }
    }

    private String hashMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
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
            throw new RuntimeException(e);
        }
    }
}

