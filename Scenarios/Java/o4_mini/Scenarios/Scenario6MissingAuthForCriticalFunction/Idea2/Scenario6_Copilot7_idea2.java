import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot7_idea2 {

    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            Boolean authenticated = session != null
                ? (Boolean) session.getAttribute("authenticated")
                : false;
            return Boolean.TRUE.equals(authenticated);
        }

        /** Simple HTML escape to prevent XSS */
        private String htmlEscape(String input) {
            if (input == null) return "";
            StringBuilder escaped = new StringBuilder();
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '&':  escaped.append("&amp;");  break;
                    case '<':  escaped.append("&lt;");   break;
                    case '>':  escaped.append("&gt;");   break;
                    case '"':  escaped.append("&quot;"); break;
                    case '\'': escaped.append("&#x27;"); break;
                    case '/':  escaped.append("&#x2F;"); break;
                    default:   escaped.append(c);
                }
            }
            return escaped.toString();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1) Auth check
            if (!ensureLoggedIn(req)) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 2) Render page
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                HttpSession session = req.getSession(false);
                String username = session != null
                    ? (String) session.getAttribute("username")
                    : "User";
                String safeUsername = htmlEscape(username);

                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head>");
                out.println("  <meta charset=\"UTF-8\">");
                out.println("  <title>Settings</title>");
                out.println("</head>");
                out.println("<body>");
                out.printf("  <h1>Welcome, %s</h1>%n", safeUsername);
                out.println("  <p>Secret settings page!</p>");
                out.println("</body>");
                out.println("</html>");
            }
        }
    }
}
