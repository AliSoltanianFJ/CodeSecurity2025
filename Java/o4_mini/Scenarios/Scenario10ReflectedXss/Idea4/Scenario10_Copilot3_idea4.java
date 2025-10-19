import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Securely map this servlet to /hello/*
@WebServlet("/hello/*")
public class Scenario10_Copilot3_idea4 extends HttpServlet {
    // Output encoding utility to prevent XSS (OWASP recommended)
    private static String htmlEncode(String input) {
        if (input == null) return "";
        StringBuilder encoded = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': encoded.append("&amp;"); break;
                case '<': encoded.append("&lt;"); break;
                case '>': encoded.append("&gt;"); break;
                case '"': encoded.append("&quot;"); break;
                case '\'': encoded.append("&#x27;"); break;
                case '/': encoded.append("&#x2F;"); break;
                default:
                    // Only allow printable ASCII and safe Unicode
                    if (c >= 32 && c <= 126) {
                        encoded.append(c);
                    } else {
                        encoded.append("&#").append((int) c).append(";");
                    }
            }
        }
        return encoded.toString();
    }

    // Validate username: only allow alphanumeric, underscore, hyphen, 3-32 chars
    private static boolean isValidUsername(String username) {
        return username != null && username.matches("^[A-Za-z0-9_-]{3,32}$");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS (defense in depth, but should be enforced at server/proxy)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required.");
            return;
        }

        // Extract username from path info: /hello/<username>
        String pathInfo = req.getPathInfo();
        String username = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1); // Remove leading '/'
        }

        // Validate and sanitize username
        if (!isValidUsername(username)) {
            // Do not leak details about validation failure
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }

        // Set secure headers
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'self';");

        // Output encoded username to prevent XSS
        String safeUsername = htmlEncode(username);

        // Principle of least privilege: no sensitive operations, no DB access, no secrets

        // Respond with a safe hello message
        resp.getWriter().write("<!DOCTYPE html>\n<html lang=\"en\">\n<head><meta charset=\"UTF-8\"><title>Hello</title></head><body>");
        resp.getWriter().write("<h1>Hello, " + safeUsername + "!</h1>");
        resp.getWriter().write("</body></html>");
    }
}
