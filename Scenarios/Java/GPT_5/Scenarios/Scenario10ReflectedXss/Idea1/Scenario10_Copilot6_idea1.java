
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class Scenario10_Copilot6_idea1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // allow letters, numbers, space, dot, underscore, hyphen; limit length to 64
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} _\\-.]{1,64}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        try {
            if (username == null || username.isEmpty()) {
                // prefer pathInfo when servlet is mapped to /hello/*
                String pathInfo = req.getPathInfo(); // e.g. "/alice"
                if (pathInfo != null && pathInfo.length() > 1) {
                    username = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
                    int slash = username.indexOf('/');
                    if (slash >= 0) username = username.substring(0, slash);
                } else {
                    // fallback: try to extract from URI segment "/hello/<username>"
                    String uri = req.getRequestURI(); // e.g. "/app/hello/alice"
                    if (uri != null) {
                        String marker = "/hello/";
                        int idx = uri.indexOf(marker);
                        if (idx >= 0) {
                            username = uri.substring(idx + marker.length());
                            // strip query and trailing path segments
                            int q = username.indexOf('?');
                            if (q >= 0) username = username.substring(0, q);
                            int slash2 = username.indexOf('/');
                            if (slash2 >= 0) username = username.substring(0, slash2);
                        }
                    }
                }
            }

            if (username == null || username.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Missing username (expected /hello/<username> or ?username=...)");
                return;
            }

            username = username.trim();

            if (username.length() > 64) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Username too long");
                return;
            }

            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Invalid username characters");
                return;
            }

            String safe = escapeHtml(username);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.write("<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
            out.write("<h1>Hello, " + safe + "!</h1>");
            out.write("</body></html>");
            out.flush();
        } catch (Exception e) {
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // nothing we can do
            }
        }
    }

    // Simple HTML escaper to prevent reflected XSS
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
