// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot9_idea2 extends HttpServlet {
    // ...existing code...

    // Allow only safe characters for a username and limit length to avoid excessive memory use.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");
    private static final int MAX_USERNAME_LENGTH = 50;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Always use UTF-8
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/html; charset=UTF-8");

        // Harden response headers against XSS/clickjacking/content sniffing
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; script-src 'none'; object-src 'none';");

        String username = null;

        try {
            // 1) Try query parameter first: /hello?username=...
            String param = req.getParameter("username");
            if (param != null && !param.isEmpty()) {
                username = param;
            } else {
                // 2) If not present, try path info for pattern: /hello/<username>
                // Typical mapping for this servlet would be /hello/*
                String pathInfo = req.getPathInfo(); // returns "/<username>" or null
                if (pathInfo != null && pathInfo.length() > 1) {
                    // Remove leading slash and URL-decode using UTF-8
                    String raw = pathInfo.substring(1);
                    try {
                        username = URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
                    } catch (IllegalArgumentException ex) {
                        // Malformed percent-encoding
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username encoding.");
                        return;
                    }
                }
            }

            // If still missing, return a 400 with a generic message (do not expose internal details)
            if (username == null || username.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username.");
                return;
            }

            // Sanitize and validate:
            // - Trim whitespace
            username = username.trim();

            // - Enforce maximum length (defensive); avoid huge allocations
            if (username.length() > MAX_USERNAME_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username too long.");
                return;
            }

            // - Validate allowed characters (reject anything unexpected to prevent XSS / injection)
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username contains invalid characters.");
                return;
            }

            // Escape for HTML output to prevent reflected XSS.
            String escaped = escapeHtml(username);

            // Safe output: minimal HTML with escaped content.
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter out = resp.getWriter()) {
                out.print("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Greeting</title></head><body>");
                out.print("Hello, ");
                out.print(escaped);
                out.print("!");
                out.print("</body></html>");
                out.flush();
            }

        } catch (Exception e) {
            // Do not reveal internal details or stack traces to the client.
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
            // Optionally log the exception server-side (not shown here to avoid exposing configuration).
        }
    }

    // Minimal HTML-escaping routine: replaces characters that have special meaning in HTML.
    // This avoids adding external dependencies while preventing reflected XSS.
    private static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#x27;");
                    break;
                case '/':
                    sb.append("&#x2F;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    // ...existing code...
}
