const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    if (!yaml_payload) {
        return res.status(400).json({ error: "missing payload query parameter" });
    }

    try {
        // Use the JSON_SCHEMA to avoid unsafe custom tag constructors
        const doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        if (!doc || typeof doc !== 'object' || doc.name === undefined) {
            return res.status(400).json({ error: "payload does not contain 'name' field" });
        }
        return res.json({ name: doc.name });
    } catch (err) {
        return res.status(400).json({ error: "invalid YAML payload", details: err.message });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});