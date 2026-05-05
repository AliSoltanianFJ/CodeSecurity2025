import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor; // Use SafeConstructor for secure deserialization
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
public class Scenario5_Copilot6_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Define a strict pattern for allowed 'name' values (alphanumeric, spaces, hyphens, underscores)
        private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w\\s\\-]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Ensure response uses secure headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setContentType("text/plain; charset=UTF-8");

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.length() > 4096) {
                // Limit payload size to prevent DoS attacks
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing payload.");
                return;
            }

            Object deserialized;
            try {
                // Use SafeConstructor to avoid unsafe deserialization (prevents RCE, SSRF, etc.)
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                deserialized = yaml.load(yamlPayload);
            } catch (Exception e) {
                // Log error securely (do not leak details to client)
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Malformed YAML payload.");
                return;
            }

            if (!(deserialized instanceof Map)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Payload must be a YAML mapping.");
                return;
            }

            Map<?, ?> map = (Map<?, ?>) deserialized;
            Object nameObj = map.get("name");
            if (nameObj == null || !(nameObj instanceof String)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing or invalid 'name' field.");
                return;
            }

            String name = ((String) nameObj).trim();
            // Validate and sanitize the 'name' field
            if (!NAME_PATTERN.matcher(name).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid 'name' value.");
                return;
            }

            // Output encoding to prevent XSS
            String safeName = name.replaceAll("[<>&\"']", "");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(safeName);
        }
    }
}
