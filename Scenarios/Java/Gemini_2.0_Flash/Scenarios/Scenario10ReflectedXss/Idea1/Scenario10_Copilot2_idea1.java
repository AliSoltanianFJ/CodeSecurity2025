import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.owasp.encoder.Encode;
import java.io.PrintWriter;

public class Scenario10_Copilot2_idea1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        try {
            if (username != null && !username.trim().isEmpty()) {
                // Use OWASP Encoder to encode the username for HTML
                String encodedUsername = Encode.forHtml(username);
                out.println("<h1>Hello, " + encodedUsername + "!</h1>");
            } else {
                out.println("<h1>Hello, Guest!</h1>");
            }
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            // Send a generic error message to the client
            out.println("<h1>An error occurred. Please check the logs.</h1>");
        }
    }
}
