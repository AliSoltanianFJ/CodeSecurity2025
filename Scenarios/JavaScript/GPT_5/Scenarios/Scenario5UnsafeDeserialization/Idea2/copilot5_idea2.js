const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// simple HTML-escape to prevent reflected XSS
function escapeHtml(str) {
  return String(str).replace(/[&<>"'\/]/g, (c) =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '/': '&#x2F;' }[c])
  );
}

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate presence and type
    if (!yaml_payload || typeof yaml_payload !== 'string') {
      return res.status(400).json({ error: 'payload query parameter is required and must be a string' });
    }

    // Limit size to prevent abusive input / potential memory issues
    const MAX_PAYLOAD_LEN = 2048; // reasonable limit for a name/short payload
    if (yaml_payload.length > MAX_PAYLOAD_LEN) {
      return res.status(413).json({ error: 'payload too large' });
    }

    let parsed;
    try {
      // Use a safe schema to avoid JS-specific tags / arbitrary code execution
      // json schema restricts to plain JSON types (no functions/constructors)
      parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
      return res.status(400).json({ error: 'invalid YAML payload' });
    }

    // Ensure parsed value is an object and not coming from prototype pollution
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return res.status(400).json({ error: 'payload must be a YAML mapping/object containing a "name" field' });
    }

    // Reject payloads that attempt to set dangerous top-level keys
    const forbiddenTopLevelKeys = ['__proto__', 'constructor', 'prototype'];
    for (const k of forbiddenTopLevelKeys) {
      if (Object.prototype.hasOwnProperty.call(parsed, k)) {
        return res.status(400).json({ error: 'invalid payload keys' });
      }
    }

    // Ensure name is an own property (not inherited) and is a string
    if (!Object.prototype.hasOwnProperty.call(parsed, 'name') || typeof parsed.name !== 'string') {
      return res.status(400).json({ error: '"name" field is required and must be a string' });
    }

    // Sanitize and limit the name value
    const MAX_NAME_LEN = 256;
    const rawName = parsed.name;
    if (rawName.length > MAX_NAME_LEN) {
      return res.status(400).json({ error: '"name" field too long' });
    }

    const safeName = escapeHtml(rawName);

    // Response with JSON content-type, do not reflect raw input
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.setHeader('Content-Security-Policy', "default-src 'none'");
    return res.json({ name: safeName });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
