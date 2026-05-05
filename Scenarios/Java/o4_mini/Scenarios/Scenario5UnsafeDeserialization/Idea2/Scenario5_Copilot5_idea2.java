import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot5_idea2 {

    public static class YamlApiServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        // limit payload to 8 KB to prevent abuse
        private static final int MAX_PAYLOAD_SIZE = 8 * 1024;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload parameter");
                return;
            }

            if (yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload too large");
                return;
            }

            // Configure SnakeYAML with SafeConstructor and strict loader options
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setAllowDuplicateKeys(false);
            loaderOptions.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML format");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML root is not a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object,Object> map = (Map<Object,Object>) loaded;

            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            String name = nameObj.toString();
            // Prevent cross-site scripting
            String safeName = StringEscapeUtils.escapeHtml4(name);

            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().print(safeName);
        }
    }
}
