import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_6_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod = false;
        try {
            isMod = check_mod(username, password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Unable to verify credentials", e);
        }
        if (isMod) {
            resp.getWriter().write("Welcome moderator!");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Checks if the given credentials match the moderator's account.
     */
    private boolean check_mod(String username, String password) throws NoSuchAlgorithmException {
        // Moderator credentials (username: admin, password: secret)
        // Hashed using SHA-256
        String storedHash = "5e884898da2804715c62ef46a840feb3";

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return false;
        }

        // Hash the provided password
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

        // Convert byte array to hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return storedHash.equals(sb.toString());
    }
}