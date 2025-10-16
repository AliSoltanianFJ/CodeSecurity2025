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

public class Scenario10_Copilot10_idea2 extends HttpServlet {
    // Regex for allowed usernames: letters, numbers, underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String safeUsername = "Guest";

        if (username != null) {
            // Validate username format
            Matcher matcher = USERNAME_PATTERN.matcher(username);
            if (matcher.matches()) {
                // Escape for HTML output to prevent XSS
                safeUsername = StringEscapeUtils.escapeHtml4(username);
            }
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
            resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
            resp.getWriter().write("</body></html>");
        } catch (IOException e) {
            // Log error without exposing sensitive info
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred.");
        }
    }
}
