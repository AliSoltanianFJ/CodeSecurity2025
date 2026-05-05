// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot1_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Scenario10.class.getName());

    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Determine username from path /hello/{username} or fallback to ?username=
        String username = null;
        try {
            String pathInfo = req.getPathInfo(); // expects mapping like /hello/*
            if (pathInfo != null && pathInfo.length() > 1) {
                // remove leading slash and URL-decode safely
                username = URLDecoder.decode(pathInfo.substring(1), StandardCharsets.UTF_8.name());
            } else {
                String param = req.getParameter("username");
                if (param != null && !param.isBlank()) {
                    username = URLDecoder.decode(param, StandardCharsets.UTF_8.name());
                }
            }
        } catch (IllegalArgumentException e) {
            // invalid percent-encoding
            LOGGER.log(Level.WARNING, "Invalid encoding in username parameter", e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username encoding");
            return;
        }

        // Basic presence check
        if (username == null || username.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        // Normalize Unicode to prevent spoofing tricks
        username = Normalizer.normalize(username, Normalizer.Form.NFKC).trim();

        // Enforce a reasonable maximum length to avoid resource issues
        final int MAX_LEN = 64;
        if (username.length() > MAX_LEN) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username too long");
            return;
        }

        // Whitelist allowed characters (alphanumeric, dot, underscore, hyphen)
        // Adjust the pattern if you need to allow international names, but validate carefully.
        if (!username.matches("^[A-Za-z0-9._-]{1," + MAX_LEN + "}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username contains invalid characters");
            return;
        }

        // Escape for HTML output to prevent reflected XSS
        String safeUsername = escapeHtml(username);

        // Response headers to improve security posture
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline';"); // minimal CSP
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");

        // Write response safely. Don't reveal internal errors or stack traces to the client.
        try (PrintWriter out = resp.getWriter()) {
            out.print("<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
            out.print("Hello, " + safeUsername + "!");
            out.print("</body></html>");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write response", e);
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate response");
            }
        }
    }

    // Minimal, fast HTML escaper - avoids external libs and covers common XSS characters
    private static String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default:
                    // drop control characters that are not printable
                    if (c >= 0x20 && c != 0x7F) {
                        sb.append(c);
                    } else {
                        sb.append('?');
                    }
            }
        }
        return sb.toString();
    }
}
// ...existing code...
