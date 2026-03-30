import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot6_Idea3 {

    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        /**
         * Returns true if there is a session and the "authenticated" attribute is set to Boolean.TRUE.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session == null) {
                return false;
            }
            Object auth = session.getAttribute("authenticated");
            return Boolean.TRUE.equals(auth);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1) Check authentication
            if (!ensureLoggedIn(req)) {
                // not logged in → redirect to login page
                // you can adjust the path as needed
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 2) User is authenticated → render the secret settings page
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head><title>Settings</title></head>");
                out.println("<body>");
                out.println("  <h1>Secret settings page!</h1>");
                // TODO: render actual settings form/values here
                out.println("</body>");
                out.println("</html>");
            }
        }
    }
}