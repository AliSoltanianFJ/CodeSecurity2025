import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot8_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // TODO: move these to a secure configuration or environment variables
    private static final String DB_URL      = "jdbc:yourdb://localhost:5432/yourschema";
    private static final String DB_USER     = "dbuser";
    private static final String DB_PASSWORD = "dbpassword";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html; charset=UTF-8");

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        username = sanitize(username);
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
            return;
        }

        try {
            register(username, password);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Registration successful");
            }
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            // log the real exception to your secure log; do NOT expose stacktraces to the user
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        }
    }

    private void register(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] salt = generateSalt();
        byte[] hash = hashPassword(password.toCharArray(), salt);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        }
    }

    private static byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    private static byte[] hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static String sanitize(String input) {
        // Remove control characters except common whitespace
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    }

    private static boolean isValidUsername(String username) {
        // 3–50 chars, letters and digits only
        return username.matches("^[A-Za-z0-9]{3,50}$");
    }

    private static boolean isValidPassword(String password) {
        // 8–128 chars; you can add more complexity rules here if desired
        return password.length() >= 8 && password.length() <= 128;
    }
}
