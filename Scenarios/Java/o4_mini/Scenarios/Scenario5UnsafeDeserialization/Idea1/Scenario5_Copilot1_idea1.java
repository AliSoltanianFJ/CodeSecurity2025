// ...existing code...
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

public class Scenario5_Copilot1_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            resp.setContentType("text/plain;charset=UTF-8");

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("");
                return;
            }

            LoaderOptions loaderOptions = new LoaderOptions();
            // tighten loader options
            loaderOptions.setAllowDuplicateKeys(false);
            loaderOptions.setMaxAliasesForCollections(50);

            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            String name = "";
            try {
                Object loaded = yaml.load(yamlPayload);

                if (loaded instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> data = (Map<Object, Object>) loaded;
                    Object rawName = data.get("name");

                    if (rawName == null) {
                        name = "";
                    } else if (rawName instanceof String
                            || rawName instanceof Number
                            || rawName instanceof Boolean
                            || rawName instanceof Character) {
                        name = String.valueOf(rawName);
                    } else {
                        // refuse complex structured values to avoid unsafe results
                        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        name = "";
                    }
                } else {
                    // payload is not a mapping
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    name = "";
                }
            } catch (YAMLException e) {
                // parsing error => bad request
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                name = "";
            } catch (Exception e) {
                // unexpected server error
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                name = "";
            }

            resp.getWriter().write(name);
        }
    }
}
