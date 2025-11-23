// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot8 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = null;

        try {
            // Prefer path-based username: /hello/<username>
            String pathInfo = req.getPathInfo(); // returns "/<username>" or null
            if (pathInfo != null && pathInfo.length() > 1) {
                username = pathInfo.substring(1); // strip leading '/'
            }

            // Fallback to query parameter ?username=...
            if (username == null || username.isEmpty()) {
                username = req.getParameter("username");
            }

            if (username == null || username.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
                return;
            }

            // Escape to prevent reflected XSS
            String safeName = escapeHtml(username);

            resp.setContentType("text/html;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.println("<h1>Hello, " + safeName + "!</h1>");
                out.println("</body></html>");
            }

        } catch (Exception e) {
            // Log server-side if you have logging; return a generic 500 to the client.
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    // Minimal HTML escaping to avoid reflected XSS (covers common characters)
    private static String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '\"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}