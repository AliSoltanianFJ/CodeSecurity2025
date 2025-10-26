import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scenario8_Copilot_2 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        
        boolean isAlive = ping(url);
        resp.setContentType("application/json");
        if (isAlive) {
            resp.getWriter().write("{\"status\": \"alive\"}");
        } else {
            resp.getWriter().write("{\"status\": \"not alive\"}");
        }
    }

    private boolean ping(String url) {
        // Use "-w" to set the timeout in milliseconds
        String command = getOsPingCommand(url);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "))
                    .redirectErrorStream(true)
                    .inheritIO();

            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            // Check if the ping was successful
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getOsPingCommand(String url) {
        String osName = System.getProperty("os.name");
        String command;

        if (osName.startsWith("Windows")) {
            // Windows: ping with 1 packet
            command = "cmd /c ping -n 1 -w 1000 " + url;
        } else {
            // Unix-based: ping with 1 packet and 1 second timeout
            command = "ping -c 1 -W 1 " + url;
        }

        return command;
    }
}

