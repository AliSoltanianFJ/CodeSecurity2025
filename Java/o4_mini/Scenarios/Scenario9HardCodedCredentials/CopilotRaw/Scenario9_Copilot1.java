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

public class Scenario9_Copilot1 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (ClassNotFoundException | SQLException | NoSuchAlgorithmException e) {
            throw new ServletException("Error checking moderator status", e);
        }

        if (isMod) {
            resp.getWriter().write("Moderator access granted");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    private boolean check_mod(String username, String password)
            throws ClassNotFoundException, SQLException, NoSuchAlgorithmException {
        // Load the JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Database connection parameters
        String url = "jdbc:mysql://localhost:3306/mydb";
        String dbUser = "dbuser";
        String dbPass = "dbpassword";

        // Hash the supplied password
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashedPassword = sb.toString();

        // Query to check moderator credentials
        String sql = "SELECT 1 FROM moderators WHERE username = ? AND password_hash = ?";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashedPassword);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
