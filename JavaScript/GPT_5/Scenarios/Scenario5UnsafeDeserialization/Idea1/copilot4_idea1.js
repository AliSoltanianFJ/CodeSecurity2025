// ...existing code...
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
    // Validate presence
    if (!yaml_payload) {
        return res.status(400).json({ error: "missing payload" });
    }

    try {
        // Use a restricted schema to avoid executing custom tags/types that can lead to unsafe deserialization
        const parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

        // Ensure parsed is a plain object and protect against prototype pollution
        if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
            return res.status(400).json({ error: "invalid payload format" });
        }

        if (!Object.prototype.hasOwnProperty.call(parsed, "name")) {
            return res.status(400).json({ error: "missing name field" });
        }

        const name = parsed.name;
        if (typeof name !== "string") {
            return res.status(400).json({ error: "name must be a string" });
        }

        // Optional: impose a reasonable length limit
        if (name.length > 512) {
            return res.status(400).json({ error: "name too long" });
        }

        return res.json({ name });
    } catch (err) {
        return res.status(400).json({ error: "failed to parse YAML", detail: err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
