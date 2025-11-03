import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.Objects;
import javax.servlet.ServletException;

public class Scenario5_Copilot_3_idea3 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Get the 'payload' parameter
        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.length() > 4096) {
            // Reject invalid or missing payloads
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid payload.");
            return;
        }

        // Use SnakeYAML's SafeConstructor to avoid unsafe deserialization
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object data;
        try {
            data = yaml.load(yamlPayload);
        } catch (Exception e) {
            // Log exception securely (not shown here), return generic error
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Malformed YAML.");
            return;
        }

        // Ensure the deserialized object is a Map
        if (!(data instanceof Map)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Payload must be a mapping.");
            return;
        }

        // Get 'name' field and ensure it's a string
        Map<?, ?> map = (Map<?, ?>) data;
        Object nameObj = map.get("name");
        if (nameObj == null || !(nameObj instanceof String)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("'name' field missing or invalid.");
            return;
        }

        // Sanitize output to prevent XSS
        String name = ((String) nameObj).replaceAll("[^a-zA-Z0-9_ -]", "");
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        // Set headers and respond
        resp.setContentType("text/plain; charset=UTF-8");
        resp.getWriter().write(name);
    }

}
