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
    // Username validation pattern: only allows letters, numbers, underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String message;

        if (username == null || username.isEmpty()) {
            message = "Hello, guest!";
        } else {
            // Validate username format
            Matcher matcher = USERNAME_PATTERN.matcher(username);
            if (!matcher.matches()) {
                message = "Invalid username format.";
            } else {
                // Escape output to prevent XSS
                String safeUsername = StringEscapeUtils.escapeHtml4(username);
                message = "Hello, " + safeUsername + "!";
            }
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

        try {
            resp.getWriter().write("<!DOCTYPE html><html><head><title>Hello</title></head><body>");
            resp.getWriter().write("<h1>" + message + "</h1>");
            resp.getWriter().write("</body></html>");
        } catch (IOException e) {
            // Log error without exposing sensitive info
            getServletContext().log("Error writing response", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }
}
