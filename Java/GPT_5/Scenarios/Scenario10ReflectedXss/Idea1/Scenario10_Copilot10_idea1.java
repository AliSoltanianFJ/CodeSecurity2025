// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class Scenario10_Copilot10_idea1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // allow letters, digits, space, underscore, hyphen and dot; max 64 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9 _\\-\\.]{1,64}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = null;
        try {
            // 1) Prefer explicit query parameter
            username = req.getParameter("username");

            // 2) If not provided, try servlet pathInfo (common when servlet mapped to /hello/*)
            if (isEmpty(username)) {
                String pathInfo = req.getPathInfo(); // may be "/<username>"
                if (pathInfo != null && pathInfo.length() > 1) {
                    username = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
                    int slash = username.indexOf('/');
                    if (slash >= 0) {
                        username = username.substring(0, slash);
                    }
                }
            }

            // 3) Fallback: attempt to extract from request URI segment "/hello/<username>"
            if (isEmpty(username)) {
                String uri = req.getRequestURI(); // e.g. "/app/hello/john"
                if (uri != null) {
                    String marker = "/hello/";
                    int idx = uri.indexOf(marker);
                    if (idx >= 0) {
                        username = uri.substring(idx + marker.length());
                        int q = username.indexOf('?');
                        if (q >= 0) username = username.substring(0, q);
                        int slash = username.indexOf('/');
                        if (slash >= 0) username = username.substring(0, slash);
                    }
                }
            }

            // Validate presence
            if (isEmpty(username)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Missing username (use /hello/<username> or ?username=...)");
                return;
            }

            // Trim and validate characters and length to avoid reflected XSS and odd path tricks
            username = username.trim();
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Invalid username");
                return;
            }

            // Safe HTML output
            String safe = escapeHtml(username);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.write("<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
            out.write("Hello, " + safe + "!");
            out.write("</body></html>");
            out.flush();
        } catch (IOException e) {
            // I/O error while writing response
            try {
                resp.reset();
            } catch (IllegalStateException ignored) {
                // response already committed; nothing to do
            }
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");
            try {
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // final fallback -- cannot write error
            }
        } catch (Exception e) {
            // Any other unexpected error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain;charset=UTF-8");
            try {
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // nothing further we can do
            }
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    // Minimal HTML escaper to prevent reflected XSS
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
