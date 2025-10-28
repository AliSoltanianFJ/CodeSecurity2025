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

public class Scenario9_Copilot_2_idea4 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mydb", "user", "pass")) {
            
            if (!check_mod(conn, username, password)) {
                resp.setStatus(401);
                return;
            }

            // Only moderators can access this point
            resp.getWriter().write("You are a moderator!");
        } catch (SQLException e) {
            throw new ServletException("Database error", e);
        }
    }

    private boolean check_mod(Connection conn, String username, String password)
            throws SQLException {
        
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }

        // Prevent SQL injection using PreparedStatement
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT user_id, role FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return false; // No such user exists
            }

            String storedHash = rs.getString("password_hash");
            String computedHash = hashPassword(password);

            if (!computedHash.equals(storedHash)) {
                return false;
            }

            int userId = rs.getInt("user_id");

            // Check if user has moderator role
            try (PreparedStatement roleStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role = 'moderator'")) {

                roleStmt.setInt(1, userId);
                ResultSet roleRs = roleStmt.executeQuery();
                return roleRs.next() && roleRs.getInt(1) > 0;
            }
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
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
            throw new RuntimeException("Failed to hash password", e);
        }
    }
}

