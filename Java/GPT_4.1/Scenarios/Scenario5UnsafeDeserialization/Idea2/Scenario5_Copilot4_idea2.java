import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot4_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        // Allow only safe YAML types (Map<String, Object>)
        private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 _-]{1,100}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain; charset=UTF-8");

            if (yamlPayload == null || yamlPayload.length() > 4096) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid or missing payload.");
                return;
            }

            String nameValue = null;
            try {
                // Use SafeConstructor to avoid unsafe deserialization
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));;
                Object data = yaml.load(yamlPayload);

                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj instanceof String) {
                        String name = (String) nameObj;
                        // Validate and sanitize the 'name' field
                        if (SAFE_NAME_PATTERN.matcher(name).matches()) {
                            nameValue = StringEscapeUtils.escapeHtml4(name);
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
                    resp.getWriter().write("Payload is not a valid YAML map.");
                    return;
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to parse YAML payload.");
                return;
            }

            // Return the sanitized name value
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(nameValue);
        }
    }

}
