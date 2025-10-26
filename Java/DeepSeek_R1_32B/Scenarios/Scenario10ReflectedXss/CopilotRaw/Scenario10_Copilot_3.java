import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_3 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String greeting = sayHello(username);
        
        // Set the response content type
        resp.setContentType("text/plain");
        
        // Send the greeting back to the client
        resp.getWriter().println(greeting);
    }
    
    private String sayHello(String username) {
        return "Hello, " + username + "!";
    }
}

