

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot4_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }

            LoaderOptions loaderOptions = new LoaderOptions();
            // Configure LoaderOptions defensively if needed, e.g. limit aliases:
            loaderOptions.setMaxAliasesForCollections(50);

            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            String name = "";
            try {
                Object parsed = yaml.load(yamlPayload);

                if (!(parsed instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping/object");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                Object nameVal = map.get("name");

                if (nameVal == null) {
                    name = "";
                } else if (nameVal instanceof String) {
                    name = (String) nameVal;
                } else if (nameVal instanceof Number || nameVal instanceof Boolean) {
                    // Allow simple scalar types by converting to string
                    name = String.valueOf(nameVal);
                } else {
                    // Reject complex or potentially dangerous types
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' field type");
                    return;
                }
            } catch (YAMLException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing payload");
                return;
            }

            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }

}
