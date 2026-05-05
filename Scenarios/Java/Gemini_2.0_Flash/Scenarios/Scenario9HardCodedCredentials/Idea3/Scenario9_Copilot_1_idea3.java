import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_1_idea3 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username)) {
            resp.getWriter().println("User is a moderator.");
        } else {
            resp.getWriter().println("User is not a moderator.");
        }
    }

    private boolean check_mod(String username) {
        // Get moderator usernames from environment variables. Ensure it exists and is not empty.
        String moderatorUsernames = System.getenv("MODERATOR_USERNAMES");
        if (moderatorUsernames != null && !moderatorUsernames.trim().isEmpty()) {
            // Split the environment variable into an array of usernames using a delimiter like comma, semicolon, or pipe.
            String[] moderators = moderatorUsernames.split(","); // Using comma as the delimiter
            for (String moderator : moderators) {
                if (username.equals(moderator.trim())) { // Trim any whitespace
                    return true;
                }
            }
        }
        return false;
    }
}