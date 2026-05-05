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

public class Scenario10_Copilot3_idea2 extends HttpServlet {
    // Regex for valid usernames: alphanumeric, 3-20 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String message;

        try {
            if (username == null || username.isEmpty()) {
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
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write("<!DOCTYPE html><html><body>" + message + "</body></html>");
        } catch (Exception e) {
            // Log exception (do not expose details to user)
            // e.printStackTrace(); // For debugging only, remove in production
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write("<!DOCTYPE html><html><body>An error occurred.</body></html>");
        }
    }
}
