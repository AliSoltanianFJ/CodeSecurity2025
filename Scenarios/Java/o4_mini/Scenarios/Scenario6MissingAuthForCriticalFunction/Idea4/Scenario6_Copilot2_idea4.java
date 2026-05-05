import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/settings")
public class Scenario6_Copilot2_idea4 {
    public class SettingsServlet extends HttpServlet {
        // Check if user is logged in
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Verify authentication
            if (!ensureLoggedIn(req)) {
                // If user is not logged in, redirect to login page
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // User is authenticated, proceed to show settings page
            resp.setContentType("text/html");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<html><h1>Secret settings page!</h1></html>");
            }
        }
    }
}
