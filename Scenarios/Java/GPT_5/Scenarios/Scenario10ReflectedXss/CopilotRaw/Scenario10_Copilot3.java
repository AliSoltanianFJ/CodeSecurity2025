import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// ...existing code...

public class Scenario10_Copilot3 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            // Try query parameter first
            String username = req.getParameter("username");

            // If not provided as query parameter, try to extract from path:
            // common mappings:
            //  - servlet mapped to /hello/*  -> pathInfo == "/<username>"
            //  - request uri contains "/hello/<username>"
            if (username == null || username.isEmpty()) {
                String pathInfo = req.getPathInfo(); // e.g. "/john" when mapped to /hello/*
                if (pathInfo != null && !pathInfo.isEmpty()) {
                    // remove leading slash if present
                    if (pathInfo.startsWith("/")) {
                        username = pathInfo.substring(1);
                    } else {
                        username = pathInfo;
                    }
                } else {
                    // fallback: try to parse from request URI in case servlet mapping differs
                    String uri = req.getRequestURI(); // e.g. "/app/hello/john"
                    if (uri != null) {
                        int idx = uri.indexOf("/hello/");
                        if (idx != -1) {
                            username = uri.substring(idx + "/hello/".length());
                            // strip any trailing path/query pieces
                            int slash = username.indexOf('/');
                            if (slash != -1) {
                                username = username.substring(0, slash);
                            }
                        }
                    }
                }
            }

            if (username == null || username.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Missing username. Use /hello/<username> or ?username=<name>");
                return;
            }

            // Escape username to prevent reflected XSS
            String safeUsername = escapeHtml(username);

            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html>");
                out.println("<head><meta charset=\"utf-8\"><title>Hello</title></head>");
                out.println("<body>");
                out.println("<h1>Hello, " + safeUsername + "!</h1>");
                out.println("</body>");
                out.println("</html>");
            }
        } catch (Exception e) {
            // Handle unexpected errors gracefully
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");
            try {
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // nothing else we can do here
            }
        }
    }

    // Simple HTML escaper to avoid reflected XSS
    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#x27;"); break;
                case '/': out.append("&#x2F;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }
    // ...existing code...
}