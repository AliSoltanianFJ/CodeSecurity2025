package scenarios; // adjust package if needed

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Safe "hello/<username>" servlet.
 *
 * Security measures:
 * - Accept username from path info (/hello/<username>) or "username" query param.
 * - Strict validation: only letters, digits, underscore, hyphen; length capped.
 * - Output as text/plain UTF-8 and HTML-escaped to prevent XSS.
 * - No deserialization or sensitive info exposure. Exceptions are logged only.
 */
public class Scenario10_Copilot6_idea2 extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Allow only these characters and limit length to prevent abuse/buffer issues.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = null;

        try {
            // Prefer path info if the servlet is mapped to /hello/*
            String pathInfo = req.getPathInfo(); // e.g. "/alice"
            if (pathInfo != null && pathInfo.length() > 1) {
                // Remove leading slash and trim
                username = pathInfo.substring(1).trim();
            }

            // Fallback to query parameter
            if ((username == null || username.isEmpty()) && req.getParameter("username") != null) {
                username = req.getParameter("username").trim();
            }

            // If still empty, reject request
            if (username == null || username.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username.");
                return;
            }

            // Validate against whitelist pattern
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
                return;
            }

            // Defensive: ensure length limit (already enforced by regex) and remove CR/LF to prevent header injection
            if (username.contains("\r") || username.contains("\n")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
                return;
            }

            // Escape output for extra safety even though we're using text/plain
            String safeUsername = escapeHtml(username);

            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("text/plain; charset=UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                out.print("Hello, ");
                out.print(safeUsername);
                out.print("!");
            }

        } catch (Exception e) {
            // Log server-side without exposing details to the client
            getServletContext().log("Error handling hello request", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
        }
    }

    // Minimal HTML escaping (no external dependencies) to prevent reflected XSS if text is later rendered as HTML.
    private static String escapeHtml(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '\"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                case '`': sb.append("&#x60;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
