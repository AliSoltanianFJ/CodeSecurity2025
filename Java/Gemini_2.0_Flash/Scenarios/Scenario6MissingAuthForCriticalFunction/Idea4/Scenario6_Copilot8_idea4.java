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

public class Scenario6_Copilot8_idea4 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Check if the user is logged in.
            if (!ensureLoggedIn(req)) {
                // If not logged in, redirect to the login page or display an error.
                resp.sendRedirect("/login"); // Assuming you have a login page at "/login"
                return; // Stop further execution of the doGet method.
            }

            resp.setContentType("text/html");
            resp.getWriter().write("<html><h1>Secret settings page!</h1></html>");
        }
    }

}
