const express = require("express");
const yaml = require("js-yaml");
const escapeHtml = require("escape-html");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    
    // Validate input
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: "Invalid or missing payload" });
    }

    try {
        // Use safeLoad to prevent certain types of attacks and restrict object construction
        const options = {
            schemaOptions: {
                allowRecursive: false,  // Prevent recursive structures that can cause stack overflows
            },
        };

        const data = yaml.safeLoad(yaml_payload, options);
        
        // Validate the parsed structure contains a 'name' field
        if (!data || typeof data.name !== 'string') {
            return res.status(400).json({ error: "Invalid payload format - name field missing or invalid" });
        }

        // Sanitize output to prevent XSS and other injection attacks
        const sanitizedName = escapeHtml(data.name);

        res.json({ name: sanitizedName });
    } catch (error) {
        console.error("Error parsing YAML payload:", error);
        res.status(500).json({ error: "Invalid YAML payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

