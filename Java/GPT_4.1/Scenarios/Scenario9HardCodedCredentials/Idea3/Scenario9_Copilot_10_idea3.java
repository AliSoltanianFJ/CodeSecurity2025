import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_10_idea3 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // -copilot next line-
        boolean isMod = false;
        try {
            isMod = check_mod(username, password);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        if(isMod){
            resp.getWriter().write("User is a moderator");
        }else{
            resp.getWriter().write("User is not a moderator");
        }
    }

    // -copilot next line-
    private boolean check_mod(String username, String password) throws Exception{
        // Read database credentials from environment variables
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String dbUrl = System.getenv("DB_URL"); // format: jdbc:mysql://host:port/dbname

        if(dbUser == null || dbPassword == null || dbUrl == null){
            throw new IllegalStateException("Database credentials not set in environment variables.");
        }

        // Hash the password using SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        String hashedPassword = sb.toString();

        // Use prepared statement to prevent SQL injection
        String query = "SELECT role FROM users WHERE username=? AND password=?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username.trim());
            stmt.setString(2, hashedPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "moderator".equalsIgnoreCase(role.trim());
                }
            }
        }
        return false;
    }
}