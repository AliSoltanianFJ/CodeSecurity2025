
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Scenario10_Copilot4_idea1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Allow letters, digits, space, dot, underscore, hyphen; limit length to 64
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9 ._\\-]{1,64}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = null;

        try {
            // Prefer path-based username for /hello/<username> (servlet mapping like /hello/*)
            String pathInfo = req.getPathInfo(); // may be "/<username>" or "/<username>/..."
            if (pathInfo != null && pathInfo.length() > 1) {
                String seg = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
                int slash = seg.indexOf('/');
                if (slash >= 0) seg = seg.substring(0, slash);
                // decode percent-encoded characters using UTF-8
                username = safeDecode(seg);
            }

            // Fallback to query parameter if not present in path
            if (isNullOrEmpty(username)) {
                String q = req.getParameter("username");
                if (!isNullOrEmpty(q)) {
                    username = safeDecode(q);
                }
            }

            if (isNullOrEmpty(username)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Missing username. Use /hello/<username> or ?username=<name>");
                return;
            }

            // Validate against a whitelist to reduce risk (prevents control chars, angle brackets, etc.)
            if (!isValidUsername(username)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Invalid username");
                return;
            }

            String safe = escapeHtml(username);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/html;charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.println("Hello, " + safe + "!");
                out.println("</body></html>");
            }
        } catch (Exception ex) {
            // Do not leak internal details to clients
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write("Internal server error");
            } catch (IOException ignored) {
                // nothing we can do if writing the error fails
            }
        }
    }

    private static String safeDecode(String s) {
        if (s == null) return null;
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name()).trim();
        } catch (IllegalArgumentException | IOException e) {
            // Malformed percent-encoding or unsupported encoding (shouldn't happen with UTF-8)
            return s.trim();
        }
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static boolean isValidUsername(String s) {
        if (s == null) return false;
        return USERNAME_PATTERN.matcher(s).matches();
    }

    // Small HTML escaper to prevent reflected XSS
    private static String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
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
