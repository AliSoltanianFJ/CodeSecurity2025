import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot3 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract username from the path "hello/<username>"
        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        String username = "world";
        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1);
        }
        resp.setContentType("text/plain");
        resp.getWriter().write("Hello, " + username + "!");
    }
}
