import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_2_idea3 extends HttpServlet {
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
            resp.getWriter().write("Welcome, moderator!");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Checks if given credentials match the moderator's account.
     */
    private boolean check_mod(String username, String password) throws NoSuchAlgorithmException {
        // Read moderator credentials from environment variables
        String storedUsername = System.getenv("MOD_USERNAME");
        String storedPasswordHash = System.getenv("MOD_PASSWORD");

        if (storedUsername == null || storedPasswordHash == null) {
            System.out.println("Moderator credentials not set in environment variables.");
            return false;
        }

        // Hash the provided password using SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

        // Convert hash to hex string for comparison
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String passwordHash = sb.toString();

        return storedUsername.equals(username) && storedPasswordHash.equals(passwordHash);
    }
}