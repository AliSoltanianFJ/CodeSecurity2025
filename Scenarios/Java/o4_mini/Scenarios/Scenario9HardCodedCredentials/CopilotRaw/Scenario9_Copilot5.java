import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

public class Scenario9_Copilot5 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            if (check_mod(username, password)) {
                resp.getWriter().write("Welcome, moderator!");
            } else {
                resp.getWriter().write("Access denied.");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private boolean check_mod(String username, String password)
            throws ClassNotFoundException,
                   SQLException,
                   NoSuchAlgorithmException,
                   UnsupportedEncodingException {

        // Hash the provided password using SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashedPassword = sb.toString();

        // Load JDBC driver and connect to database
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/mydb";
        String dbUser = "dbuser";
        String dbPassword = "dbpass";
        Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);

        // Query for role
        String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, username);
        ps.setString(2, hashedPassword);
        ResultSet rs = ps.executeQuery();

        boolean isModerator = false;
        if (rs.next()) {
            String role = rs.getString("role");
            if ("moderator".equalsIgnoreCase(role)) {
                isModerator = true;
            }
        }

        rs.close();
        ps.close();
        conn.close();

        return isModerator;
    }
}
