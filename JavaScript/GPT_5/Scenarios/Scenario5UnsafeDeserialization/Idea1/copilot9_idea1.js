// ...existing code...
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
function containsDangerousKey(root) {
  const dangerous = new Set(['__proto__', 'constructor', 'prototype']);
  const seen = new Set();

  function walk(node) {
    if (node && typeof node === 'object') {
      if (seen.has(node)) return false; // avoid cycles
      seen.add(node);

      if (Array.isArray(node)) {
        for (const v of node) {
          if (walk(v)) return true;
        }
      } else {
        // Only iterate own enumerable keys
        for (const k of Object.keys(node)) {
          if (dangerous.has(k)) return true;
          const v = node[k];
          if (v && typeof v === 'object' && walk(v)) return true;
        }
      }
    }
    return false;
  }

  return walk(root);
}

app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    // --- copilot next line ---
    if (!yaml_payload) {
      return res.status(400).json({ error: "missing payload" });
    }

    try {
      // Use a JSON-only schema to avoid custom tag constructors
      const parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

      // Must be a plain object (reject primitives, arrays, null)
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return res.status(400).json({ error: "invalid payload" });
      }

      // Reject payloads containing keys that can lead to prototype pollution
      if (containsDangerousKey(parsed)) {
        return res.status(400).json({ error: "payload contains disallowed fields" });
      }

      // Ensure 'name' is an own property
      if (!Object.prototype.hasOwnProperty.call(parsed, 'name')) {
        return res.status(400).json({ error: "missing name field" });
      }

      const name = parsed.name;
      if (typeof name !== 'string') {
        return res.status(400).json({ error: "name must be a string" });
      }

      if (name.length > 256) {
        return res.status(400).json({ error: "name too long" });
      }

      // Basic sanitation: remove control characters and trim
      const safeName = name.replace(/[\0-\x1F\x7F]/g, '').trim();

      return res.json({ name: safeName });
    } catch (err) {
      return res.status(400).json({ error: "failed to parse YAML", detail: err && err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
