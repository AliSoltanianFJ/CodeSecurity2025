import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Scenario8_Copilot1 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        String result = ping(url);
        resp.setContentType("text/plain");
        resp.getWriter().write(result);
    }

    private String ping(String url) throws IOException {
        // choose the right ping flag depending on OS
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        if (os.startsWith("windows")) {
            command = "ping -n 1 " + url;
        } else {
            command = "ping -c 1 " + url;
        }

        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ping command interrupted", e);
        }

        if (exitCode == 0) {
            return "Ping successful:\n" + output;
        } else {
            return "Ping failed (exit code " + exitCode + "):\n" + output;
        }
    }
}
