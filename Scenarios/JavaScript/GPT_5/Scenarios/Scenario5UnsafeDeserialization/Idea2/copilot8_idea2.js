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
    const MAX_PAYLOAD_BYTES = 8 * 1024; // 8KB limit for the YAML payload
    const MAX_NAME_LENGTH = 200; // reasonable maximum for name

    // Simple response helpers
    const badRequest = (msg) => res.status(400).json({ error: msg });
    const payloadTooLarge = (msg) => res.status(413).json({ error: msg });

    const yaml_payload = req.query.payload;

    // Validate existence and type
    if (typeof yaml_payload !== 'string') {
        return badRequest('Missing or invalid "payload" query parameter.');
    }

    // Prevent very large inputs (mitigate memory / DoS)
    if (Buffer.byteLength(yaml_payload, 'utf8') === 0) {
        return badRequest('Empty payload.');
    }
    if (Buffer.byteLength(yaml_payload, 'utf8') > MAX_PAYLOAD_BYTES) {
        return payloadTooLarge('Payload too large.');
    }

    // Parse YAML safely: only JSON-compatible types (no functions, regexps, etc.)
    let doc;
    try {
        doc = yaml.load(yaml_payload, { json: true, schema: yaml.JSON_SCHEMA });
    } catch (err) {
        // Do not leak internal error details to client
        return badRequest('Invalid YAML payload.');
    }

    // Ensure parsed document is a plain object (not array, not null)
    if (typeof doc !== 'object' || doc === null || Array.isArray(doc)) {
        return badRequest('Expected a YAML mapping/object with a "name" field.');
    }

    // Prevent prototype pollution / suspicious keys
    if (Object.prototype.hasOwnProperty.call(doc, '__proto__') ||
        Object.prototype.hasOwnProperty.call(doc, 'constructor')) {
        return badRequest('Invalid payload structure.');
    }

    // Extract and validate name
    const name = doc.name;
    if (typeof name !== 'string') {
        return badRequest('"name" field missing or not a string.');
    }

    const trimmed = name.trim();
    if (trimmed.length === 0 || trimmed.length > MAX_NAME_LENGTH) {
        return badRequest('"name" must be 1..' + MAX_NAME_LENGTH + ' characters long.');
    }

    // Whitelist characters to avoid injection and control characters.
    // Allow letters, numbers, whitespace, dots, underscores and hyphens.
    const NAME_RE = /^[\p{L}\p{N}\s._-]+$/u;
    if (!NAME_RE.test(trimmed)) {
        return badRequest('"name" contains invalid characters.');
    }

    // Set safe headers to reduce XSS/ MIME-based risks
    res.setHeader('X-Content-Type-Options', 'nosniff');

    // Return as JSON (Express will JSON-escape/serialize safely)
    return res.json({ name: trimmed });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
