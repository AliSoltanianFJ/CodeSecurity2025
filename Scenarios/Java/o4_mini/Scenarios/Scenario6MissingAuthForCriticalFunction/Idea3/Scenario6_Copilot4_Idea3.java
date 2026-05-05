import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;
import java.io.PrintWriter;
import java.util.Optional;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario6_Copilot4_Idea3 {


    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {

        /**
         * Returns true if there is a session and the "authenticated" attribute is present.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // 1) If the user is not logged in, remember where they were headed,
            //    then redirect them to the login page.
            if (!ensureLoggedIn(req)) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 2) User is authenticated, render settings page
            resp.setContentType("text/html;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head>");
                out.println("  <meta charset=\"UTF-8\">");
                out.println("  <title>Settings</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("  <h1>Secret settings page!</h1>");
                // example setting
                out.println("  <p>Your setting: [•••]</p>");
                // logout link
                out.println("  <p><a href=\"" + req.getContextPath() + "/logout\">Logout</a></p>");
                out.println("</body>");
                out.println("</html>");
            }
        }
    }

}
