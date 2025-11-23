import java.io.IOException;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot5_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Allow only these characters and limit length to prevent abuse and injections
    private static final Pattern SAFE_USERNAME = Pattern.compile("^[A-Za-z0-9._\\-]{1,32}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = null;

        try {
            // Prefer REST-style endpoint: /hello/<username>
            String pathInfo = req.getPathInfo(); // e.g. "/hello/john"
            if (pathInfo != null) {
                // Normalize to avoid trickery with unicode
                pathInfo = Normalizer.normalize(pathInfo, Normalizer.Form.NFKC);
                // Expecting path like "/hello/<username>" or "/<username>" depending on mapping
                // Try to extract a trailing segment if it looks like /hello/<name>
                String[] parts = pathInfo.split("/");
                if (parts.length >= 2) {
                    // If mapped as /hello then parts[1] == "hello" and parts[2] == username
                    int idx = 1;
                    if ("hello".equalsIgnoreCase(parts[1]) && parts.length >= 3) {
                        idx = 2;
                    }
                    if (parts.length > idx && parts[idx] != null && !parts[idx].isEmpty()) {
                        username = parts[idx];
                    }
                }
            }

            // Fallback to query parameter ?username=...
            if (username == null) {
                String param = req.getParameter("username");
                if (param != null && !param.isEmpty()) {
                    username = Normalizer.normalize(param, Normalizer.Form.NFKC);
                }
            }

            if (username == null || username.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writePlain(resp, "Missing username. Use /hello/<username> or ?username=<name>");
                return;
            }

            // Reject control characters and CR/LF to prevent header injection
            if (username.indexOf('\r') >= 0 || username.indexOf('\n') >= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writePlain(resp, "Invalid username.");
                return;
            }

            // Validate allowed characters and length
            if (!SAFE_USERNAME.matcher(username).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writePlain(resp, "Username contains invalid characters or is too long.");
                return;
            }

            // Escape for HTML output to prevent reflected XSS
            String safe = htmlEscape(username);

            // Produce a minimal, safe HTML response
            resp.setContentType("text/html; charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.print("<!doctype html><html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
                out.print("<h1>Hello, " + safe + "</h1>");
                out.print("</body></html>");
            }
        } catch (Exception e) {
            // Do not expose internal details to the client; log server-side if desired
            log("Error handling request in Scenario10", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writePlain(resp, "An internal error occurred.");
        }
    }

    // Small utility to write plain text responses safely
    private void writePlain(HttpServletResponse resp, String message) throws IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print(message);
        }
    }

    // Basic HTML escaping; sufficient for simple output without external libs
    private static String htmlEscape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '/': sb.append("&#x2F;"); break;
                default:
                    // Avoid writing non-printable control characters
                    if (Character.isISOControl(c)) {
                        // Skip control characters
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
