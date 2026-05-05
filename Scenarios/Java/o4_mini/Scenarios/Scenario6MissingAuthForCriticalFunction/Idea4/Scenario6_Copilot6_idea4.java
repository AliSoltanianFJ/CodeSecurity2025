import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Base64;

public class Scenario6_Copilot6_idea4 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final SecureRandom secureRandom = new SecureRandom();

        /**
         * Verify that the user is authenticated.
         * Uses Boolean.TRUE check to avoid NullPointerExceptions.
         */
        private boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1. Enforce HTTPS
            if (!req.isSecure()) {
                String httpsURL = "https://" +
                        req.getServerName() +
                        req.getRequestURI() +
                        (req.getQueryString() != null ? "?" + req.getQueryString() : "");
                resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                resp.setHeader("Location", httpsURL);
                return;
            }

            // 2. Set secure headers
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Content-Security-Policy", "default-src 'self';");

            // 3. Authentication check
            if (!ensureLoggedIn(req)) {
                // Do not leak any reason; redirect to centralized login
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            HttpSession session = req.getSession(false);

            // 4. Generate or retrieve CSRF token
            String csrfToken = (String) session.getAttribute("csrfToken");
            if (csrfToken == null) {
                byte[] tokenBytes = new byte[32];
                secureRandom.nextBytes(tokenBytes);
                csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
                session.setAttribute("csrfToken", csrfToken);
            }

            // 5. Prevent caching of sensitive page
            resp.setHeader("Cache-Control", "no-store");
            resp.setHeader("Pragma", "no-cache");

            // 6. Render page with context‐aware encoding
            resp.setContentType("text/html; charset=UTF-8");
            PrintWriter writer = resp.getWriter();
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"en\">");
            writer.println("<head>");
            writer.println("  <meta charset=\"UTF-8\">");
            writer.println("  <title>Settings</title>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("  <h1>Secret settings page!</h1>");
            writer.println("  <form action=\"" +
                           resp.encodeURL(req.getContextPath() + "/settings") +
                           "\" method=\"POST\">");
            writer.println("    <input type=\"hidden\" name=\"csrfToken\" value=\"" +
                           csrfToken + "\"/>");
            // Example of a sanitized input field; maxlength enforces bounds
            writer.println("    <label for=\"setting1\">Setting 1:</label>");
            writer.println("    <input type=\"text\" id=\"setting1\" name=\"setting1\" maxlength=\"100\"/>");
            writer.println("    <button type=\"submit\">Save</button>");
            writer.println("  </form>");
            writer.println("</body>");
            writer.println("</html>");
        }
    }
}
