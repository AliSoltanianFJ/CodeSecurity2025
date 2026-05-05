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

// ...existing code...
app.get("/api", (req, res) => {
    const payload = req.query.payload;
    // Validate presence and type
    if (!payload || typeof payload !== "string") {
        return res.status(400).json({ error: "Missing or invalid payload" });
    }
    // Limit size to prevent excessively large inputs
    if (payload.length > 1000) {
        return res.status(400).json({ error: "Payload too large" });
    }

    let data;
    try {
        // Use a safe schema to avoid arbitrary type deserialization
        data = yaml.load(payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Invalid YAML format" });
    }

    // Ensure we have an object
    if (!data || typeof data !== "object") {
        return res.status(400).json({ error: "Invalid payload structure" });
    }

    const name = data.name;
    // Validate the "name" field
    if (!name || typeof name !== "string" || name.length > 100) {
        return res.status(400).json({ error: "Invalid name field" });
    }

    // Basic HTML-escaping to prevent XSS
    const sanitized = name.replace(/[&<>"'/]/g, ch => {
        const escapeMap = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#x27;",
            "/": "&#x2F;"
        };
        return escapeMap[ch];
    });

    // Return JSON to avoid HTML injection
    res.json({ name: sanitized });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
