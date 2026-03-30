import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot2_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1. Read the raw YAML payload from the "payload" parameter
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            // 2. Parse it safely
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));
            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML");
                return;
            }

            // 3. Verify it’s a map and extract "name"
            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Expected a YAML mapping");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<Object, Object> data = (Map<Object, Object>) loaded;

            Object nameObj = data.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }
            String name = ((String) nameObj);

            // 4. Return the name
            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }
}