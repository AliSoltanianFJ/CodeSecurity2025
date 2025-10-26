import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_10 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getPathInfo().substring(1); // Extract the username from the path
        String greeting = "Hello, " + username + "!";  // Create the greeting message
        
        // Set the response content type and send the greeting
        resp.setContentType("text/plain");
        resp.getWriter().println(greeting);
    }
}

