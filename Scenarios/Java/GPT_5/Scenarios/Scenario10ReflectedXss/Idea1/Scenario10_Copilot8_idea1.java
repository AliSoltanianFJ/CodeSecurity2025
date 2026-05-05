// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot8_idea1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            String username = req.getParameter("username");

            // Try to extract username from path like /hello/<username> when parameter is missing
            if (username == null || username.isEmpty()) {
                String pathInfo = req.getPathInfo(); // may be "/<username>" if servlet mapped to /hello/*
                if (pathInfo != null && pathInfo.length() > 1) {
                    username = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
                    int slash = username.indexOf('/');
                    if (slash >= 0) {
                        username = username.substring(0, slash);
                    }
                } else {
                    // Fallback: try to find "/hello/" segment in the request URI
                    String uri = req.getRequestURI();
                    String marker = "/hello/";
                    int idx = (uri != null) ? uri.indexOf(marker) : -1;
                    if (idx >= 0) {
                        username = uri.substring(idx + marker.length());
                        int q = username.indexOf('?');
                        if (q >= 0) username = username.substring(0, q);
                        int slash2 = username.indexOf('/');
                        if (slash2 >= 0) username = username.substring(0, slash2);
                    }
                }
            }

            if (username == null || username.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    out.write("Missing username. Use /hello/<username> or ?username=<name>");
                }
                return;
            }

            // Normalize and limit length
            username = username.trim();
            if (username.length() > 100) {
                username = username.substring(0, 100);
            }

            // Allow only a safe subset of characters to avoid unexpected input (prevents some injection risks)
            String sanitized = sanitizeUsername(username);

            // Escape for HTML to avoid reflected XSS
            String safe = escapeHtml(sanitized);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html>");
                out.println("<head><meta charset=\"utf-8\"><title>Hello</title></head>");
                out.println("<body>");
                out.println("<h1>Hello, " + safe + "!</h1>");
                out.println("</body>");
                out.println("</html>");
            }
        } catch (Exception e) {
            // Ensure any exception results in a clean HTTP 500 response
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");
            try {
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // nothing else we can do
            }
        }
    }

    // Keep only alphanumerics, space, hyphen, underscore, dot
    private static String sanitizeUsername(String s) {
        if (s == null) return "";
        // Remove control characters and any character not in the allowed set
        return s.replaceAll("[^\\p{Alnum} \\-_.]", "");
    }

    // Lightweight HTML escaper to avoid reflected XSS (no external libs required)
    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
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
// ...existing code...
