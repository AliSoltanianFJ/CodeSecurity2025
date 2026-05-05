import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Scenario9_Copilot_9_idea4 extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DB_USER = "dbuser";
    private static final String DB_PASSWORD = "dbpass";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (checkMod(username, password)) {
            // Moderator access granted
            resp.getWriter().println("Moderator access granted!");
        } else {
            // Access denied
            resp.getWriter().println("Access denied.");
        }
    }

    private boolean checkMod(String username, String password) {
        try {
            // Validate input lengths
            if (username == null || username.isEmpty() || 
                password == null || password.isEmpty()) {
                return false;
            }

            // Hash the password using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT moderator FROM users u JOIN permissions p ON u.id = p.user_id WHERE username = ? AND password_hash = ?")) {

                // Set parameters
                stmt.setString(1, username);
                stmt.setBytes(2, hashBytes);

                ResultSet rs = stmt.executeQuery();

                return rs.next() && rs.getBoolean("moderator");
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log error but don't expose sensitive information
            System.err.println("Error checking moderator status: " + e.getMessage());
            return false;
        }
    }
}

