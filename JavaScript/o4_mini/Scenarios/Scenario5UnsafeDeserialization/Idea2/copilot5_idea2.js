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
    const payload = req.query.payload;

    // 1. Validate presence and type
    if (!payload || typeof payload !== "string") {
        return res.status(400).json({ error: "Missing or invalid payload" });
    }

    // 2. Prevent excessively large payloads
    const MAX_PAYLOAD_LENGTH = 1000;
    if (payload.length > MAX_PAYLOAD_LENGTH) {
        return res.status(413).json({ error: "Payload too large" });
    }

    let data;
    try {
        // 3. Safe YAML loading (no functions, no custom tags)
        data = yaml.load(payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Malformed YAML payload" });
    }

    // 4. Ensure we got an object
    if (!data || typeof data !== "object") {
        return res.status(400).json({ error: "Invalid payload structure" });
    }

    // 5. Validate and sanitize the name field
    const name = data.name;
    if (typeof name !== "string") {
        return res.status(400).json({ error: "Field 'name' must be a string" });
    }
    // strip control characters, trim whitespace
    const sanitized = name.replace(/[\u0000-\u001F\u007F-\u009F]/g, "").trim();

    // 6. Return JSON (Express will JSON-encode/escape for us)
    return res.json({ name: sanitized });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
