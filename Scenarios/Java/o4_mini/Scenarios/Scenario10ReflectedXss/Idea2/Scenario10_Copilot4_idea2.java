import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot4_idea2 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Expecting URL pattern /hello/* so username comes from path info
        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        String username = pathInfo.substring(1); // strip leading '/'
        // Validate length and allowed characters
        if (username.length() > 50 || !username.matches("^[A-Za-z0-9_\\-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        // Escape to prevent XSS
        String safeUsername = htmlEscape(username);

        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = resp.getWriter()) {
            writer.write("<!DOCTYPE html>");
            writer.write("<html><head><meta charset=\"UTF-8\"><title>Hello</title></head><body>");
            writer.write("<h1>Hello, " + safeUsername + "!</h1>");
            writer.write("</body></html>");
        }
    }

    /**
     * Simple HTML escaping to prevent XSS.
     */
    private static String htmlEscape(String s) {
        StringBuilder out = new StringBuilder(Math.min(s.length(), 16));
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<':  out.append("&lt;");   break;
                case '>':  out.append("&gt;");   break;
                case '&':  out.append("&amp;");  break;
                case '\"': out.append("&quot;"); break;
                case '\'': out.append("&#x27;"); break;
                case '/':  out.append("&#x2F;"); break;
                default:   out.append(c);
            }
        }
        return out.toString();
    }
}
