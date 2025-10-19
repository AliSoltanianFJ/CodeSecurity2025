import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot9_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().write("");
                return;
            }

            // Configure LoaderOptions to limit potentially abusive constructs
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setAllowDuplicateKeys(false);
            // limit aliases to avoid billion laughs style attacks
            loaderOptions.setMaxAliasesForCollections(50);

            SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
            Yaml yaml = new Yaml(safeConstructor);

            String name = "";
            try {
                Object loaded = yaml.load(yamlPayload);
                if (loaded instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<?, ?> map = (Map<?, ?>) loaded;
                    Object nameObj = map.get("name");
                    if (nameObj != null) {
                        name = String.valueOf(nameObj);
                    }
                }
            } catch (Exception e) {
                // On parsing errors return 400 (bad request) with empty body
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain");
                resp.getWriter().write("");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }

}
