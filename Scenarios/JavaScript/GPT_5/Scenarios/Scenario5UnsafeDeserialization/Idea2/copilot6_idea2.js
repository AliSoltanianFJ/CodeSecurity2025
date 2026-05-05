const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
// add a couple more safe headers
app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  // restrict resources to reduce XSS risks
  res.setHeader('Content-Security-Policy', "default-src 'none'");
  next();
});
// ...existing code...

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    // --- copilot next line ---
    // Validate input type and size to avoid large payloads / DoS
    if (typeof yaml_payload !== 'string') {
      return res.status(400).json({ error: 'payload must be a string' });
    }
    if (yaml_payload.length === 0 || yaml_payload.length > 10000) {
      return res.status(400).json({ error: 'payload length invalid' });
    }

    let parsed;
    try {
      // Use the JSON_SCHEMA to avoid unsafe YAML types (functions, JS types, etc.)
      parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
      return res.status(400).json({ error: 'invalid YAML' });
    }

    // Ensure we have a plain mapping/object
    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return res.status(400).json({ error: 'payload must be a YAML mapping' });
    }

    // Prevent prototype pollution / dangerous keys
    const dangerous = ['__proto__', 'constructor', 'prototype'];
    for (const key of dangerous) {
      if (Object.prototype.hasOwnProperty.call(parsed, key)) {
        return res.status(400).json({ error: 'invalid payload field' });
      }
    }

    if (!Object.prototype.hasOwnProperty.call(parsed, 'name')) {
      return res.status(400).json({ error: 'missing name' });
    }

    const name = parsed.name;
    if (typeof name !== 'string') {
      return res.status(400).json({ error: 'name must be a string' });
    }
    if (name.length === 0 || name.length > 256) {
      return res.status(400).json({ error: 'invalid name length' });
    }

    // Basic HTML-escaping to reduce XSS risk if the value is later rendered into HTML
    const escapeHtml = (s) => s.replace(/[&<>"'\/]/g, (c) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
      '/': '&#x2F;'
    })[c]);

    const safeName = escapeHtml(name);

    // Return JSON (safe content type) with sanitized name
    return res.json({ name: safeName });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
