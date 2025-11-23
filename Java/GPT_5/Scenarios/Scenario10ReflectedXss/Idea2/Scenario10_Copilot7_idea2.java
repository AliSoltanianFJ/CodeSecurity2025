// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot7_idea2 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract username from path "hello/<username>" (expects servlet mapped to /hello/*)
        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username in path.");
            return;
        }

        String rawUsername;
        try {
            rawUsername = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
            // Decode percent-encoding safely
            rawUsername = URLDecoder.decode(rawUsername, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            // Don't reveal internal details to the client
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Validate input: allow only a limited safe character set and length
        // Prevents XSS, control chars, very long input (buffer overflow not an issue in Java but limit size anyway)
        final int MAX_LEN = 32;
        if (rawUsername.isEmpty() || rawUsername.length() > MAX_LEN) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username must be 1 to " + MAX_LEN + " characters.");
            return;
        }
        // Only allow letters, digits, underscore and hyphen
        if (!rawUsername.matches("^[A-Za-z0-9_-]{1,32}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username contains invalid characters.");
            return;
        }

        // Escape for HTML output to prevent reflected XSS
        String safeUsername = escapeHtml(rawUsername);

        // Security headers
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/html;charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self'; img-src 'self'; script-src 'none';");
        resp.setHeader("Referrer-Policy", "no-referrer");

        resp.setStatus(HttpServletResponse.SC_OK);

        // Send a minimal HTML response
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!doctype html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("<meta charset=\"utf-8\"/>");
            out.println("<title>Hello</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Hello, " + safeUsername + "!</h1>");
            out.println("</body>");
            out.println("</html>");
        } catch (IOException e) {
            // Generic error; do not leak internal state
            resp.reset();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to produce response.");
        }
    }

    // Minimal HTML escaper: escapes the small set of characters needed to prevent XSS in text nodes/attributes.
    private static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
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
// ...existing code...
