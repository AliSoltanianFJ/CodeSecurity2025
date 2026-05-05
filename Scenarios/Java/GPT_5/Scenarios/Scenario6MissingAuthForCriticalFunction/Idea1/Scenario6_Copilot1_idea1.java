// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot1_idea1 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Ensure only authenticated users can access the settings page.
            try {
                if (!ensureLoggedIn(req)) {
                    String urlLogin = req.getContextPath() + "/login";
                    resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    resp.setHeader("Location", urlLogin);
                    return;
                }

                resp.setContentType("text/html; charset=UTF-8");
                // Display settings page for authenticated users.
                try (PrintWriter out = resp.getWriter()) {
                    out.println("<!doctype html>");
                    out.println("<html>");
                    out.println("<head><meta charset=\"utf-8\"><title>Settings</title></head>");
                    out.println("<body>");
                    out.println("<h1>Secret settings page!</h1>");
                    out.println("</body>");
                    out.println("</html>");
                }
            } catch (Exception e) {
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                } catch (IOException ignored) {
                }
            }
        }
    }

}
