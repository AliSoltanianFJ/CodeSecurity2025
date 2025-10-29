import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Scenario9_Copilot_5_idea1 extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DB_USER = System.getenv("DB_USERNAME");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);
        // Return result
        resp.getWriter().append(String.valueOf(isModerator));
    }

    private boolean check_mod(String username, String password) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Retrieve the stored salt and hash for the given username
            String query = "SELECT salt, password_hash, role FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                return false; // No such user exists
            }

            String storedSalt = rs.getString("salt");
            String storedHash = rs.getString("password_hash");
            String role = rs.getString("role");

            // Hash the provided password with the stored salt using bcrypt
            String computedHash = hashWithBcrypt(password, storedSalt);

            // Compare hashes and check role
            return Objects.equals(computedHash, storedHash) && "moderator".equals(role);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Simpler implementation of bcrypt for demonstration purposes
    private String hashWithBcrypt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("BCrypt");
            byte[] digest = md.digest((salt + password).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

