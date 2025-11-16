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
    // --- copilot next line ---
    if (!yaml_payload) {
        return res.status(400).json({ error: "missing payload" });
    }

    let doc;
    try {
        // Parse using a restricted schema to avoid arbitrary type constructors
        doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "invalid YAML", details: err.message });
    }

    const name = doc && typeof doc === "object" ? doc.name : undefined;
    if (typeof name !== "string") {
        return res.status(400).json({ error: 'name field missing or not a string' });
    }

    return res.json({ name });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});