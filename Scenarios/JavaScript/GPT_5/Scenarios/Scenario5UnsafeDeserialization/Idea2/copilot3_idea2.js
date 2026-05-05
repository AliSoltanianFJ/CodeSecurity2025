const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Basic security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('Content-Security-Policy', "default-src 'none'");
  next();
});

/**
 * Small HTML-escape helper to prevent XSS when reflecting values.
 */
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/\//g, '&#x2F;');
}

/**
 * deserialize the incoming YAML payload and return the field "name"
 * Security measures:
 *  - validate presence and type of payload
 *  - limit payload byte size to avoid large memory/DoS
 *  - use js-yaml FAILSAFE_SCHEMA to avoid arbitrary JS types (functions, etc.)
 *  - prohibit prototype pollution and require a plain object
 *  - validate and sanitize the "name" field before returning
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    if (!yaml_payload || typeof yaml_payload !== 'string') {
      return res.status(400).json({ error: 'Missing or invalid payload' });
    }

    // Prevent very large payloads (protect against memory/DoS)
    const maxBytes = 8 * 1024; // 8 KiB
    if (Buffer.byteLength(yaml_payload, 'utf8') > maxBytes) {
      return res.status(413).json({ error: 'Payload too large' });
    }

    try {
      // Use failsafe schema to avoid loading arbitrary JS types (no functions, no regex, etc.)
      const parsed = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });

      // Must be an object (mapping) containing a "name" property
      if (parsed === null || typeof parsed !== 'object') {
        return res.status(400).json({ error: 'YAML must contain a mapping/object' });
      }

      // Ensure it's a plain object (defend against prototype tampering)
      const proto = Object.getPrototypeOf(parsed);
      if (proto !== Object.prototype && proto !== null) {
        return res.status(400).json({ error: 'Unsupported YAML structure' });
      }

      // Prevent prototype pollution keys
      if (Object.prototype.hasOwnProperty.call(parsed, '__proto__') ||
          Object.prototype.hasOwnProperty.call(parsed, 'constructor')) {
        return res.status(400).json({ error: 'Invalid keys in payload' });
      }

      const name = parsed.name;

      // Validate the name field
      if (typeof name !== 'string' && typeof name !== 'number') {
        return res.status(400).json({ error: 'Missing or invalid "name" field' });
      }

      // Convert to string and sanitize for safe output
      const safeName = escapeHtml(String(name));

      // Return JSON with explicit content-type (JSON responses are less prone to XSS than
      // reflected HTML, but we still sanitize values)
      res.type('application/json').status(200).json({ name: safeName });
    } catch (err) {
      // Generic error message to avoid leaking internals
      return res.status(400).json({ error: 'Invalid YAML payload' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
