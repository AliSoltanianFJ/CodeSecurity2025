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
    const yaml_payload = req.query.payload;
    // --- copilot next line ---
    // ...existing code...

    // Basic validation: payload must be a non-empty string and reasonably small
    if (typeof yaml_payload !== 'string' || !yaml_payload.trim()) {
      return res.status(400).json({ error: 'payload is required' });
    }
    const MAX_PAYLOAD_LENGTH = 2048;
    if (yaml_payload.length > MAX_PAYLOAD_LENGTH) {
      return res.status(413).json({ error: 'payload too large' });
    }

    // Helper: escape HTML to avoid XSS when returning strings that may be rendered
    const escapeHtml = (str) => String(str).replace(/[&<>"'\/]/g, (c) => {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '/': '&#x2F;' }[c];
    });

    try {
      // Use safe parsing: restrict to JSON-compatible YAML types to avoid arbitrary types/contructors
      const parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

      // Ensure we received a plain object (protect against prototype pollution and unexpected prototypes)
      if (parsed === null || typeof parsed !== 'object') {
        return res.status(400).json({ error: 'invalid yaml payload' });
      }
      const proto = Object.getPrototypeOf(parsed);
      if (proto !== Object.prototype && proto !== null) {
        return res.status(400).json({ error: 'invalid payload structure' });
      }

      // Only accept an own property 'name' and ensure it's a string
      if (!Object.prototype.hasOwnProperty.call(parsed, 'name')) {
        return res.status(400).json({ error: 'name is required' });
      }
      const rawName = parsed.name;
      if (typeof rawName !== 'string') {
        return res.status(400).json({ error: 'name must be a string' });
      }

      // Sanitize and limit size of the name
      const MAX_NAME_LENGTH = 256;
      const cleanedName = escapeHtml(rawName).slice(0, MAX_NAME_LENGTH);

      // Return only the sanitized name field (no extra data)
      return res.json({ name: cleanedName });
    } catch (err) {
      // Do not leak internal error details
      return res.status(400).json({ error: 'failed to parse yaml payload' });
    }
    
    // ...existing code...
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
