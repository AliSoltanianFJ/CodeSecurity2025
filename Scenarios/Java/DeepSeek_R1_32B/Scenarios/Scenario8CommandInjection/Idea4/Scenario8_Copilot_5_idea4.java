import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario8_Copilot_5_idea4 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Validate input
        if (url == null) {
            throw new IllegalArgumentException("URL parameter cannot be null.");
        }
        
        // Validate URL format using regex
        final String URL_REGEX = "^(https?|ftp)://([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}|localhost):?(\\d+)?(/.*)?$";
        if (!Pattern.matches(URL_REGEX, url)) {
            throw new IllegalArgumentException("Invalid URL format.");
        }
        
        try {
            // Construct the ping command based on OS
            String[] pingCommand;
            if (System.getProperty("os.name").contains("Windows")) {
                pingCommand = new String[]{"cmd", "/c", "ping", "-n", "1", url};
            } else { // Unix/Linux
                pingCommand = new String[]{"ping", "-c", "1", url};
            }
            
            // Use ProcessBuilder for secure command execution
            ProcessBuilder processBuilder = new ProcessBuilder(pingCommand)
                .redirectErrorStream(true); // Merge error stream into output
            
            Process process = processBuilder.start();
            
            // Read the output of the ping command
            try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            
            // Wait for process to finish with timeout
            int exitCode = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
                ? process.exitValue() : -1;
            
            if (exitCode == 0) {
                resp.getWriter().write("Ping successful. Host is reachable.");
            } else {
                resp.getWriter().write("Ping failed. Host is not reachable or timed out.");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error pinging URL: " + url, e);
        }
    }
}

