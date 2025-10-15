import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor; // Use SafeConstructor to prevent unsafe deserialization
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
public class Scenario5_Copilot7_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        // Define a strict pattern for allowed 'name' values (alphanumeric, spaces, hyphens, underscores, max 100 chars)
        private static final Pattern NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Ensure response uses secure headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store");
            resp.setContentType("text/plain; charset=UTF-8");

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                // Do not leak internal details
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing or empty payload.");
                return;
            }

            String nameValue = null;
            try {
                // Use SafeConstructor to avoid unsafe deserialization (prevents RCE, SSRF, etc.)
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                // Only allow deserialization to Map<String, Object>
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj instanceof String) {
                        String name = (String) nameObj;
                        // Validate and sanitize the 'name' field
                        if (NAME_PATTERN.matcher(name).matches()) {
                            nameValue = name;
                        } else {
                            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            resp.getWriter().write("Invalid 'name' value.");
                            return;
                        }
                    } else {
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("'name' field missing or not a string.");
                        return;
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Payload must be a YAML mapping.");
                    return;
                }
            } catch (Exception e) {
                // Log exception securely (do not expose stack trace to user)
                // e.g., use a secure logging framework (not shown here)
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid YAML payload.");
                return;
            }

            // Return the sanitized 'name' value
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(nameValue);
        }
    }

}
