// ...existing code...
const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  // restrict inline scripts/styles and reduce XSS surface
  res.setHeader('Content-Security-Policy', "default-src 'none'; frame-ancestors 'none'");
  next();
});

// helper: escape HTML to prevent XSS in any rendered strings
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/\//g, '&#x2F;');
}

// helper: detect prototype-pollution keys anywhere in an object/array
function hasPrototypePollution(obj) {
  const dangerous = new Set(['__proto__', 'constructor', 'prototype']);
  const stack = [obj];
  while (stack.length) {
    const cur = stack.pop();
    if (cur && typeof cur === 'object') {
      for (const key of Object.keys(cur)) {
        if (dangerous.has(key)) return true;
        const val = cur[key];
        if (val && typeof val === 'object') stack.push(val);
      }
    }
  }
  return false;
}

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Input validation: must exist and be a string
    if (typeof yaml_payload !== 'string') {
      return res.status(400).json({ error: 'payload query parameter is required and must be a string' });
    }

    // Limit payload size to prevent excessive memory usage
    const MAX_PAYLOAD_LEN = 10 * 1024; // 10 KB
    if (yaml_payload.length === 0 || yaml_payload.length > MAX_PAYLOAD_LEN) {
      return res.status(413).json({ error: 'payload size invalid' });
    }

    // Disallow control characters that could be used for tricking parsers
    if (/[\x00-\x08\x0B\x0C\x0E-\x1F]/.test(yaml_payload)) {
      return res.status(400).json({ error: 'payload contains disallowed control characters' });
    }

    let parsed;
    try {
      // Use JSON_SCHEMA to restrict to JSON-compatible types and avoid custom JS types
      parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
      // Do not leak internal error details
      return res.status(400).json({ error: 'invalid YAML payload' });
    }

    // Ensure top-level is a plain object (not null, not array)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return res.status(400).json({ error: 'payload must deserialize to an object' });
    }

    // Prevent prototype pollution
    if (hasPrototypePollution(parsed)) {
      return res.status(400).json({ error: 'payload contains disallowed keys' });
    }

    // Only accept a "name" field that is a string
    if (!Object.prototype.hasOwnProperty.call(parsed, 'name') || typeof parsed.name !== 'string') {
      return res.status(400).json({ error: 'name field is required and must be a string' });
    }

    // Further validate name length and characters
    const name = parsed.name.trim();
    if (name.length === 0 || name.length > 200) {
      return res.status(400).json({ error: 'name length invalid' });
    }

    // Sanitize the value to avoid reflected XSS, return as JSON (safe) but still escape any HTML-like content
    const safeName = escapeHtml(name);

    // Return as JSON only; do not expose internal details
    return res.json({ name: safeName });
    
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
