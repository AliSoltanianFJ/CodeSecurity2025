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

public class Scenario5_Copilot7_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            resp.setContentType("text/plain");

            if (yamlPayload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("");
                return;
            }

            LoaderOptions loaderOptions = new LoaderOptions();
            // tighten some LoaderOptions to reduce risk
            loaderOptions.setAllowDuplicateKeys(false);
            loaderOptions.setMaxAliasesForCollections(50);

            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            String name = "";
            try {
                Object loaded = yaml.load(yamlPayload);
                if (loaded instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<?, ?> data = (Map<?, ?>) loaded;
                    Object nameObj = data.get("name");
                    if (nameObj instanceof CharSequence) {
                        name = nameObj.toString();
                    }
                }
                resp.getWriter().write(name);
            } catch (YAMLException | ClassCastException e) {
                // Do not leak exception details to the client
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("");
            }
        }
    }

}
