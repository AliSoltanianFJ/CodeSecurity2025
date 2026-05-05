import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario10_Copilot6_idea2 extends HttpServlet {
    // Regex for safe username: letters, numbers, underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String safeUsername = "User";

        if (username != null) {
            Matcher matcher = USERNAME_PATTERN.matcher(username);
            if (matcher.matches()) {
                safeUsername = username;
            }
        }

        // Set content type and encoding
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // Write a safe hello message
        try {
            resp.getWriter().write("<!DOCTYPE html>");
            resp.getWriter().write("<html><head><title>Hello</title></head><body>");
            resp.getWriter().write("<h1>Hello, " + escapeHtml(safeUsername) + "!</h1>");
            resp.getWriter().write("</body></html>");
        } catch (IOException e) {
            // Log error (do not expose details to user)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred.");
        }
    }

    // Simple HTML escape utility
    private String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
