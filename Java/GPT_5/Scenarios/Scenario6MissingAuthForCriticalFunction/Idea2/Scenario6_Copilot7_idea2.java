// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Scenario6_Copilot7_idea2 {
    // ...existing code...
    @WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final int MAX_PARAM_LENGTH = 50;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Require explicit Boolean.TRUE and non-null session
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        private static String htmlEscape(String s) {
            if (s == null) return "";
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
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Authentication check
                if (!ensureLoggedIn(req)) {
                    // Redirect to a safe login URL within the same application context
                    String loginPath = req.getContextPath() + "/login";
                    resp.setStatus(HttpServletResponse.SC_FOUND);
                    resp.setHeader("Location", resp.encodeRedirectURL(loginPath));
                    return;
                }

                // Validate and sanitize optional user input (section parameter)
                String rawSection = Optional.ofNullable(req.getParameter("section")).orElse("general");
                if (rawSection.length() > MAX_PARAM_LENGTH) {
                    rawSection = rawSection.substring(0, MAX_PARAM_LENGTH);
                }
                // Whitelist known sections only
                String section = "general";
                switch (rawSection) {
                    case "privacy":
                    case "notifications":
                    case "security":
                    case "general":
                        section = rawSection;
                        break;
                    default:
                        section = "general";
                }

                // Security headers to reduce risk of XSS/Clickjacking and caching of sensitive data
                resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                resp.setHeader("Pragma", "no-cache");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("Content-Security-Policy", "default-src 'self'");

                resp.setContentType("text/html; charset=UTF-8");
                resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

                PrintWriter out = resp.getWriter();
                out.write("<!doctype html>");
                out.write("<html><head><meta charset=\"UTF-8\"><title>Settings</title></head><body>");
                out.write("<h1>Secret settings page!</h1>");
                // Always escape any user-controlled values before writing to the response
                out.write("<div>Section: " + htmlEscape(section) + "</div>");
                // Do NOT display any sensitive information such as session identifiers or internal details
                out.write("</body></html>");
                out.flush();
            } catch (Exception e) {
                // Log server-side, but do NOT expose stack traces or internal info to the client
                getServletContext().log("Error processing settings request", e);
                try {
                    resp.reset();
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().write("An internal error occurred.");
                } catch (IOException ignored) {
                    // last-resort: nothing more we can do
                }
            }
        }
    }
    // ...existing code...
}
