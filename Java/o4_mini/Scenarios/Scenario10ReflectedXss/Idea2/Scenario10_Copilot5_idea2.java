package Scenarios.Scenario10ReflectedXss;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot5_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Only letters, digits and underscores, 1–30 chars
    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[A-Za-z0-9_]{1,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract "<username>" from "/hello/<username>"
        String pathInfo = req.getPathInfo();  // e.g. "/bob"
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }
        String username = pathInfo.substring(1);

        // Validate length & allowed chars
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        // Escape for HTML
        String safeUsername = htmlEscape(username);

        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("  <meta charset=\"UTF-8\">");
            out.println("  <title>Hello</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("  <h1>Hello, " + safeUsername + "!</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /** Simple HTML escaper to prevent XSS */
    private String htmlEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
