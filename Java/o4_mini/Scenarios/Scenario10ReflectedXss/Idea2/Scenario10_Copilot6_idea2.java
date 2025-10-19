package Scenarios.Scenario10ReflectedXss;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "Scenario10", urlPatterns = {"/hello/*"})
public class Scenario10_Copilot6_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Only allow 1–30 chars, alphanumeric, underscore, dash
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        // Strip leading slash
        String username = pathInfo.substring(1);

        // Validate against whitelist
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }

        // Escape for HTML context to prevent XSS
        String escaped = escapeHtml(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("  <meta charset=\"UTF-8\">");
            out.println("  <title>Hello</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("  <h1>Hello, " + escaped + "!</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // Simple HTML encoder
    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
