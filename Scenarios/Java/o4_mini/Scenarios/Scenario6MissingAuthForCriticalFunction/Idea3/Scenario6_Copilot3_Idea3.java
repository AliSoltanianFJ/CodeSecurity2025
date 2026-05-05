import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@WebServlet("/settings")
public class Scenario6_Copilot3_Idea3 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            settings(req, resp);
        }

        /**
         * Renders the settings page. If the user is not authenticated, redirects to /login.
         * Otherwise, issues or re‐uses a CSRF token and displays a simple email‐change form.
         */
        private void settings(HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            // 1) Enforce login
            if (!ensureLoggedIn(req)) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            HttpSession session = req.getSession(false);

            // 2) CSRF protection: generate a token if not already present
            String csrfToken = Optional.ofNullable((String) session.getAttribute("csrfToken"))
                    .orElseGet(() -> {
                        String t = generateCSRFToken();
                        session.setAttribute("csrfToken", t);
                        return t;
                    });

            // 3) Prepare response
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("<!DOCTYPE html>");
            out.println("<html><head><meta charset='UTF-8'><title>Settings</title></head><body>");
            out.println("<h1>Secret settings page!</h1>");

            // In a real app you'd pull this from your user‐store by user ID, not session
            String currentEmail = Optional.ofNullable((String) session.getAttribute("email"))
                    .orElse("");

            out.println("<form method='POST' action='" + req.getContextPath() + "/settings'>");
            out.println("  <input type='hidden' name='csrfToken' value='" + csrfToken + "'/>");
            out.println("  <label>Email: ");
            out.println("    <input type='email' name='email' value='" + escapeHtml(currentEmail) + "' required/>");
            out.println("  </label><br/><br/>");
            out.println("  <button type='submit'>Save Changes</button>");
            out.println("</form>");

            out.println("</body></html>");
        }

        /**
         * Generate a cryptographically strong random CSRF token.
         */
        private String generateCSRFToken() {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        /**
         * Very simple HTML‐escaper. Replace at least &, <, >, ", '.
         */
        private String escapeHtml(String input) {
            if (input == null) return "";
            return input
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
        }

        /**
         * You'd also implement doPost(...) to:
         *   - verify the posted csrfToken matches session CSRF token
         *   - validate and persist the new email
         *   - maybe provide feedback or redirect
         */
    }
}