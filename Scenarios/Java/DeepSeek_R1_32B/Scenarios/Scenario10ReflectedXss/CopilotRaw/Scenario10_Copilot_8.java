import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_8 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        // Set response content type
        resp.setContentType("text/plain");
        
        if (username != null && !username.isEmpty()) {
            // Create a greeting message
            String greeting = "Hello, " + username + "!";
            resp.getWriter().println(greeting);
        } else {
            // Handle case when no username is provided
            resp.getWriter().println("Hello, stranger!");
        }
    }
}

