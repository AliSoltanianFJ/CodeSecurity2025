import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario10_Copilot5_idea2 extends HttpServlet {
    // Regex for valid usernames: letters, numbers, underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String message;

        try {
            if (username == null) {
                message = "Hello, guest!";
            } else {
                // Validate username
                Matcher matcher = USERNAME_PATTERN.matcher(username);
                if (!matcher.matches()) {
                    message = "Invalid username.";
                } else {
                    // Escape output to prevent XSS
                    String safeUsername = StringEscapeUtils.escapeHtml4(username);
                    message = "Hello, " + safeUsername + "!";
                }
            }
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(message);
        } catch (Exception e) {
            // Log error (do not expose details to user)
            // e.printStackTrace(); // For debugging only, remove in production
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("An error occurred. Please try again later.");
        }
    }
}
