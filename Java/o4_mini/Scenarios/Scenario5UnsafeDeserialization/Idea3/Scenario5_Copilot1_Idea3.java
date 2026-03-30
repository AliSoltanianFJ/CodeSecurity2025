import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;

public class Scenario5_Copilot1_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
                throws ServletException, IOException {
            // 1) Read raw YAML payload
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            // 2) Parse with a SafeConstructor (no arbitrary Java types)
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (RuntimeException e) {
                // malformed YAML
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
                return;
            }

            // 3) Validate that we got a mapping
            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML top-level must be a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            // 4) Return the name
            String name = ((String) nameObj);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(name);
        }
    }
}