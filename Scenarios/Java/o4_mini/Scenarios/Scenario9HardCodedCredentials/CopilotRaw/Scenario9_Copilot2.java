import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        try {
            if (check_mod(username, password)) {
                resp.getWriter().println("Welcome, moderator!");
            } else {
                resp.getWriter().println("Access denied");
            }
        } catch (ClassNotFoundException | SQLException | NoSuchAlgorithmException e) {
            throw new ServletException("Error checking moderator login", e);
        }
    }

    private boolean check_mod(String username, String password)
            throws ClassNotFoundException, SQLException, NoSuchAlgorithmException {
        // Hash the provided password
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        String passwordHash = sb.toString();

        // Load JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");
        // Hard-coded database credentials (vulnerability demonstration)
        String url    = "jdbc:mysql://localhost:3306/appdb";
        String dbUser = "dbuser";
        String dbPass = "dbpassword";

        String sql = "SELECT COUNT(*) FROM moderators WHERE username = ? AND password_hash = ?";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
