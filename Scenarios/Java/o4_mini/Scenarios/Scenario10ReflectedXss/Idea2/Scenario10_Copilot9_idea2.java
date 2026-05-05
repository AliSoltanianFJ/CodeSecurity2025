import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot9_idea2 extends HttpServlet {
    // Regular expression for a safe username: only letters, numbers, underscores, 3-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String safeUsername;

        // Validate and sanitize input
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            safeUsername = "Guest";
        } else {
            safeUsername = username;
        }

        // Set content type and encoding to prevent XSS
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        // Output a safe hello message
        resp.getWriter().write("<!DOCTYPE html>");
        resp.getWriter().write("<html><head><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + escapeHtml(safeUsername) + "!</h1>");
        resp.getWriter().write("</body></html>");
    }

    // Simple HTML escape to prevent XSS
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
