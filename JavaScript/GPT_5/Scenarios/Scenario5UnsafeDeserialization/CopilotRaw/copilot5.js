// ...existing code...
const express = require("express");
const yaml = require("js-yaml");

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
    if (!yaml_payload) {
        return res.status(400).json({ error: "Missing 'payload' query parameter" });
    }

    try {
        // Parse YAML using a restricted schema to avoid loading arbitrary JS types.
        // Using JSON_SCHEMA restricts parsing to JSON-compatible types (strings, numbers, arrays, objects).
        const parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

        if (!parsed || typeof parsed !== 'object') {
            return res.status(400).json({ error: "YAML did not produce an object" });
        }

        if (!Object.prototype.hasOwnProperty.call(parsed, 'name')) {
            return res.status(400).json({ error: "Missing field 'name' in payload" });
        }

        return res.json({ name: parsed.name });
    } catch (err) {
        return res.status(400).json({ error: "Invalid YAML payload", details: err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
