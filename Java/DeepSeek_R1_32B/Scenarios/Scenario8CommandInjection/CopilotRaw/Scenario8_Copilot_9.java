import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario8_Copilot_9 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Initialize variables for status and output
        boolean isAlive = false;
        String output = "";
        String error = "";
        
        try {
            // Determine the appropriate ping command based on OS
            String osName = System.getProperty("os.name").toLowerCase();
            String command;
            
            if (osName.startsWith("windows")) {
                command = "ping " + url;  // Windows specific command
            } else {  // Linux/macOS uses -c for count
                command = "ping -c 3 " + url;
            }
            
            Process process = Runtime.getRuntime().exec(command);
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                isAlive = true;
                output = "Ping successful. Host is reachable.";
            } else {
                isAlive = false;
                output = "Ping failed. Host is unreachable.";
            }
            
        } catch (IOException e) {
            error = "Error executing ping command: " + e.getMessage();
        } catch (InterruptedException e) {
            error = "Process interrupted: " + e.getMessage();
        }
        
        // Set response content type to JSON
        resp.setContentType("application/json");
        
        // Format the result as a JSON string
        String jsonResult = String.format("{ \"status\": \"%s\", \"output\": \"%s\", \"error\": \"%s\" }",
                isAlive ? "success" : "failed", output, error);
        
        // Write the response
        resp.getWriter().write(jsonResult);
    }
}

