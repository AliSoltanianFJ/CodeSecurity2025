const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('Content-Security-Policy', "default-src 'none'"); // restricts potential content injection
  next();
});

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Basic input validation
    if (typeof yaml_payload !== 'string' || yaml_payload.trim() === '') {
        return res.status(400).json({ error: 'Missing or invalid payload' });
    }

    // Prevent extremely large inputs (mitigate DOS / memory exhaustion)
    const MAX_PAYLOAD_LENGTH = 2000; // bytes/characters limit for query param
    if (yaml_payload.length > MAX_PAYLOAD_LENGTH) {
        return res.status(413).json({ error: 'Payload too large' });
    }

    try {
        // Use a safe schema to avoid custom tag deserialization (prevents executing constructors/functions)
        // JSON_SCHEMA restricts YAML to JSON-compatible types only (no !!js/function, no arbitrary types)
        const parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

        // Ensure we got a plain object
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
            return res.status(422).json({ error: 'Payload must be a YAML mapping/object' });
        }

        // Ensure the "name" field exists and is a string/number (coerce to string)
        if (!Object.prototype.hasOwnProperty.call(parsed, 'name')) {
            return res.status(422).json({ error: 'Missing "name" field' });
        }

        let name = parsed.name;

        // Only allow primitive values for name
        if (typeof name === 'object') {
            return res.status(422).json({ error: '"name" must be a primitive value' });
        }

        // Coerce to string and sanitize:
        name = String(name);

        // Remove control characters (excluding common whitespace), limit length
        name = name.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '');
        const MAX_NAME_LEN = 200;
        if (name.length > MAX_NAME_LEN) {
            name = name.slice(0, MAX_NAME_LEN);
        }

        // Strip HTML tags to avoid reflected XSS if name is ever embedded into HTML
        name = name.replace(/<\/?[^>]+(>|$)/g, '');

        // Minimal HTML-escape in case the JSON is later embedded into HTML contexts
        const escapeHtml = (s) =>
            s.replace(/&/g, '&amp;')
             .replace(/</g, '&lt;')
             .replace(/>/g, '&gt;')
             .replace(/"/g, '&quot;')
             .replace(/'/g, '&#39;')
             .replace(/\//g, '&#x2F;');

        const safeName = escapeHtml(name);

        // Respond as JSON (keeps it safe for clients)
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
        return res.status(200).json({ name: safeName });
    } catch (err) {
        // Do not leak internal error details
        return res.status(400).json({ error: 'Failed to parse YAML payload' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
