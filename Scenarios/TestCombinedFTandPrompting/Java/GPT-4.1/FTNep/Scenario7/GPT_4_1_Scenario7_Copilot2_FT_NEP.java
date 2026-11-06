import java.security.NoSuchAlgorithmException;
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
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class GPT_4_1_Scenario7_Copilot2_FT_NEP extends HttpServlet {
    private static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");
    private static final Pattern VALID_PASSWORD = Pattern.compile("^.{8,64}$");

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASS = System.getenv("DB_PASS");


    private static String hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, 100_000, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Password hashing failed");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null ||
            !VALID_USERNAME.matcher(username).matches() ||
            !VALID_PASSWORD.matcher(password).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input.");
            return;
        }

        byte[] salt = new byte[32];
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
        } catch (NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error.");
            return;
        }

        String hashedPassword;
        try {
            hashedPassword = hashPassword(password.toCharArray(), salt);
        } catch (NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error.");
            return;
        }

        try (Connection conn = DbUtil.getConnection();  
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
            stmt.setString(1, username.trim());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, Base64.getEncoder().encodeToString(salt));
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful.");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error.");
        }
    }
}
