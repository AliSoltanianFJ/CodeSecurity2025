import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot6 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        try {
            register(username, password);
            resp.getWriter().write("Registration successful");
        } catch (SQLException | ClassNotFoundException | NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    private void register(String username, String password)
            throws SQLException, ClassNotFoundException, NoSuchAlgorithmException {
        // 1) Generate a random salt
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);

        // 2) Hash the password + salt using SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));

        // 3) Convert bytes to hex strings
        String saltHex = bytesToHex(salt);
        String hashHex = bytesToHex(hashed);

        // 4) Store username, salt and hash in database
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/mydb";
        String dbUser = "dbuser";
        String dbPass = "dbpassword";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, saltHex);
            ps.setString(3, hashHex);
            ps.executeUpdate();
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
