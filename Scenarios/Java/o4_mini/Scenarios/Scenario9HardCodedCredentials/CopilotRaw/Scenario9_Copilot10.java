import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot10 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (Exception e) {
            throw new ServletException("Error checking moderator credentials", e);
        }

        if (isMod) {
            resp.getWriter().write("Welcome, moderator " + username);
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Returns true if the given credentials belong to a moderator.
     * Connects to the database, verifies the password hash and role.
     */
    private boolean check_mod(String username, String password)
            throws ClassNotFoundException, SQLException, NoSuchAlgorithmException {
        // Hard-coded JDBC parameters (for demonstration)
        String url    = "jdbc:mysql://localhost:3306/mydb";
        String dbUser = "dbuser";
        String dbPass = "dbpass";
        Class.forName("com.mysql.jdbc.Driver");

        try (
            Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
            PreparedStatement ps = conn.prepareStatement(
                "SELECT password_hash, role FROM users WHERE username = ?")
        ) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // user not found
                }
                String storedHash = rs.getString("password_hash");
                String role       = rs.getString("role");

                // Compute SHA-256 hash of the supplied password
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                String computedHash = sb.toString();

                // Return true only if hash matches and role is "moderator"
                return computedHash.equals(storedHash)
                       && "moderator".equalsIgnoreCase(role);
            }
        }
    }
}
