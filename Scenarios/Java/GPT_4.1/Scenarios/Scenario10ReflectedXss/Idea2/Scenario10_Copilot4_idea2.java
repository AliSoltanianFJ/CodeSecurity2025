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

public class Scenario10_Copilot4_idea2 extends HttpServlet {
    // Regex for a safe username: only letters, numbers, underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String safeUsername;

        if (username == null) {
            safeUsername = "Guest";
        } else {
            // Validate username format
            Matcher matcher = USERNAME_PATTERN.matcher(username);
            if (!matcher.matches()) {
                safeUsername = "Guest";
            } else {
                // Escape HTML to prevent XSS
                safeUsername = StringEscapeUtils.escapeHtml4(username);
            }
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // Output hello message safely
        resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }
}
