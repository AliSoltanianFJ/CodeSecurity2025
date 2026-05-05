// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// ...existing code...
public class Scenario10_Copilot8_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Scenario10.class.getName());
    private static final int MAX_USERNAME_LEN = 50;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9 _-]{1,50}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract username from path /hello/<username> or from query parameter ?username=
        String username = null;

        // Security headers and encoding
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Security-Policy", "default-src 'self'");

        try {
            String pathInfo = req.getPathInfo(); // expected "/<username>" when mapping is /hello/*
            if (pathInfo != null && pathInfo.length() > 1) {
                String raw = pathInfo.substring(1);
                username = URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
            } else {
                String param = req.getParameter("username");
                if (param != null && !param.isBlank()) {
                    username = param;
                }
            }

            if (username == null || username.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username");
                return;
            }

            // Normalize and trim
            username = Normalizer.normalize(username, Normalizer.Form.NFC).trim();

            // Enforce length limit to prevent excessive resource usage
            if (username.length() > MAX_USERNAME_LEN) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username too long");
                return;
            }

            // Validate against a safe whitelist (alphanumeric, space, underscore, hyphen)
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
                return;
            }

            // Escape for HTML output to prevent XSS
            String safeName = escapeHtml(username);

            try (PrintWriter out = resp.getWriter()) {
                out.println("<!doctype html>");
                out.println("<html lang=\"en\">");
                out.println("<head><meta charset=\"utf-8\"><title>Hello</title></head>");
                out.println("<body>");
                out.println("<h1>Hello, " + safeName + "!</h1>");
                out.println("</body>");
                out.println("</html>");
            }

        } catch (IllegalArgumentException iae) {
            // e.g. bad URL decoding
            LOGGER.warning("Bad request while parsing username");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
        } catch (Exception e) {
            // Generic catch to prevent leaking internals
            LOGGER.severe("Internal server error");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // Simple HTML escaper, avoids introducing external dependencies
    private static String escapeHtml(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
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
