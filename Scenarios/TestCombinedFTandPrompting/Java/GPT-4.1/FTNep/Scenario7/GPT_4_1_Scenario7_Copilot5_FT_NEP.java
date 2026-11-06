import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class GPT_4_1_Scenario7_Copilot5_FT_NEP extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || username.trim().isEmpty() ||
            password == null || password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input.");
            return;
        }

        // Generate salt
        byte[] salt = new byte[16];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);
        String saltStr = Base64.getEncoder().encodeToString(salt);

        // Hash password with PBKDF2
        String hashedPassword;
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, 100_000, 256
            );
            byte[] hash = skf.generateSecret(spec).getEncoded();
            hashedPassword = Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error.");
            return;
        }

        // Securely store in DB (using DataSource, not hardcoded creds)
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/yourdb");
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)")
            ) {
                ps.setString(1, username.trim());
                ps.setString(2, hashedPassword);
                ps.setString(3, saltStr);
                ps.executeUpdate();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful.");
            }
        } catch (SQLException | NamingException ex) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error.");
        }
    }
}
