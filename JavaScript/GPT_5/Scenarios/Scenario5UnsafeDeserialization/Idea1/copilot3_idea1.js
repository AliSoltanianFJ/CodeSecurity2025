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
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: "missing payload" });
    }

    // Limit payload size to mitigate resource exhaustion attacks
    const MAX_PAYLOAD_LENGTH = 10 * 1024; // 10 KB
    if (yaml_payload.length > MAX_PAYLOAD_LENGTH) {
        return res.status(400).json({ error: "payload too large" });
    }

    try {
        // Use a JSON-only schema to avoid executing custom YAML tags/types
        const parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

        // Ensure we got a plain object (not array, null, primitive, etc.)
        if (Object.prototype.toString.call(parsed) !== '[object Object]') {
            return res.status(400).json({ error: "invalid payload" });
        }

        // Reject dangerous top-level keys that could indicate prototype pollution
        const forbiddenKeys = new Set(['__proto__', 'constructor', 'prototype']);
        for (const k of Object.keys(parsed)) {
            if (forbiddenKeys.has(k)) {
                return res.status(400).json({ error: "invalid payload keys" });
            }
        }

        // Only accept an own 'name' property
        if (!Object.prototype.hasOwnProperty.call(parsed, 'name')) {
            return res.status(400).json({ error: "missing name field" });
        }

        const name = parsed.name;
        if (typeof name !== 'string') {
            return res.status(400).json({ error: "name must be a string" });
        }

        const trimmed = name.trim();
        if (trimmed.length === 0 || trimmed.length > 200) {
            return res.status(400).json({ error: "invalid name" });
        }

        // Strip control characters
        const safeName = trimmed.replace(/[\x00-\x1F\x7F]/g, '');

        return res.json({ name: safeName });
    } catch (err) {
        return res.status(400).json({ error: "failed to parse yaml" });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
