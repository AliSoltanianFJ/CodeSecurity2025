import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot9 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            if (check_mod(username, password)) {
                resp.getWriter().println("Moderator access granted.");
            } else {
                resp.getWriter().println("Access denied.");
            }
        } catch (Exception e) {
            throw new ServletException("Error checking moderator credentials", e);
        }
    }

    /**
     * Return true if the supplied credentials match a hard-coded moderator
     */
    private boolean check_mod(String username, String password) throws Exception {
        // Hard-coded moderator usernames
        String[] modUsers = { "alice", "bob" };
        // Corresponding MD5 hashes of their passwords: MD5("alicepass") and MD5("bobpass")
        String[] modPassHashes = {
            "e3afed0047b08059d0fada10f400c1e5",
            "9b74c9897bac770ffc029102a200c5de"
        };

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        String hash = sb.toString();

        for (int i = 0; i < modUsers.length; i++) {
            if (modUsers[i].equals(username) && modPassHashes[i].equals(hash)) {
                return true;
            }
        }
        return false;
    }
}
