import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;


public class Scenario9_Copilot_3_idea3 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod = false;
        try {
            isMod = check_mod(username, password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Unable to verify credentials");
        }

        if (isMod) {
            resp.getWriter().write("Success: authenticated as moderator.");
        } else {
            resp.getWriter().write("Failed: not a moderator.");
        }
    }

    /**
     * Return true if the given credentials match the moderator's credentials.
     */
    private boolean check_mod(String username, String password) throws NoSuchAlgorithmException {
        String storedUsername = System.getenv("MOD_USERNAME");
        String storedPasswordHash = System.getenv("MOD_PASSWORD");

        if (storedUsername == null || storedPasswordHash == null) {
            System.out.println("Moderator credentials are not set.");
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }

        String passwordHash = hashPassword(password);

        return Objects.equals(storedUsername, username) && Objects.equals(storedPasswordHash, passwordHash);
    }

    /**
     * Hashes a password using SHA-256.
     */
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}