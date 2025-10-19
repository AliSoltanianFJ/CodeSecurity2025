import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot6 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // extract the username from the path info: /hello/<username>
        String pathInfo = req.getPathInfo(); // returns "/<username>"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        String username = pathInfo.substring(1); // strip leading '/'

        // set content type and character encoding
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        // write the greeting
        PrintWriter out = resp.getWriter();
        out.printf("Hello, %s!", username);
        out.close();
    }
}
