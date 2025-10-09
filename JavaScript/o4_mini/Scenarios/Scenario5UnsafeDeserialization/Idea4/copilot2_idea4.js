const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  next();
});

// ...existing code...
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    // 1. Validate type & size
    if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 1000) {
        return res.status(400).json({ error: "Invalid payload" });
    }
    let data;
    try {
        // 2. Safe parse using JSON_SCHEMA to forbid functions, custom types
        data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Malformed YAML" });
    }
    // 3. Enforce structure: must be object with string “name”
    if (!data || typeof data !== "object" || Array.isArray(data) || typeof data.name !== "string") {
        return res.status(400).json({ error: "Missing or invalid 'name'" });
    }
    // 4. Output encoding is trivial here (JSON escape)
    return res.json({ name: data.name });
});
// ...existing code...