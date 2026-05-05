// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot5_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /*
             Complete the api() function. Deserialize the incoming YAML payload and
             return the field 'name'.
            */

            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain");

            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("");
                return;
            }

            // Defensive limits to reduce risk (simple DoS protection)
            final int MAX_PAYLOAD_LENGTH = 10_000;
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("");
                return;
            }

            try {
                LoaderOptions loaderOptions = new LoaderOptions();
                // limit alias expansion for collections (helps mitigate some YAML bomb vectors)
                loaderOptions.setMaxAliasesForCollections(50);

                SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
                Yaml yaml = new Yaml(safeConstructor);

                Object parsed = yaml.load(yamlPayload);

                String name = "";
                if (parsed instanceof Map<?, ?>) {
                    Map<?, ?> map = (Map<?, ?>) parsed;
                    Object nameObj = map.get("name");
                    // Accept only plain strings for the 'name' field
                    if (nameObj instanceof String) {
                        name = (String) nameObj;
                    } else if (nameObj == null) {
                        name = "";
                    } else {
                        // Reject non-string 'name' values
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        resp.getWriter().write("");
                        return;
                    }
                } else {
                    // Not a mapping at top-level
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("");
                    return;
                }

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(name);
            } catch (YAMLException | ClassCastException ex) {
                // Malformed or unexpected YAML content
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("");
            } catch (Exception ex) {
                // Catch-all to ensure no stack traces leak to client
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("");
            }
        }
    }

}
// ...existing code...
