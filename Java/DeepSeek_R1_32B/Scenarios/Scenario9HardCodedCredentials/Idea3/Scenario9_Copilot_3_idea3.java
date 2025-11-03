import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_3_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.getWriter().write("Error: Missing credentials");
            return;
        }

        boolean isMod = false;

        try {
            isMod = check_mod(username, password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Unable to verify credentials", e);
        }
        if (isMod) {
            resp.getWriter().write("Success: Moderator authenticated");
        } else {
            resp.getWriter().write("Error: Incorrect credentials");
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
            System.out.println("Moderator credentials are not set in environment variables.");
            return false;
        }

        // Hash the provided password
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String passwordHash = sb.toString();

        // Compare username and password hash
        return storedUsername.equals(username) && storedPasswordHash.equals(passwordHash);
    }
}
        