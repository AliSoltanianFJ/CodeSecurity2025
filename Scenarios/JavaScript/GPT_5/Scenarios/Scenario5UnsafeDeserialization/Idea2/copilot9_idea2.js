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
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Input validation
    if (typeof yaml_payload !== "string" || yaml_payload.trim().length === 0) {
        return res.status(400).json({ error: "Missing or invalid 'payload' parameter." });
    }

    // Prevent extremely large inputs (helps mitigate certain DoS / buffer exhaustion vectors)
    const MAX_PAYLOAD_LENGTH = 2000; // characters
    if (yaml_payload.length > MAX_PAYLOAD_LENGTH) {
        return res.status(413).json({ error: "Payload too large." });
    }

    // Helper: ensure object is a plain object (not instance of something unexpected)
    function isPlainObject(obj) {
        return Object.prototype.toString.call(obj) === "[object Object]" &&
               (Object.getPrototypeOf(obj) === Object.prototype || Object.getPrototypeOf(obj) === null);
    }

    // Helper: detect prototype-pollution keys anywhere in the object tree
    function containsProtoPollutionKeys(obj) {
        const forbidden = new Set(["__proto__", "prototype", "constructor"]);
        const stack = [obj];
        while (stack.length) {
            const current = stack.pop();
            if (!isPlainObject(current)) continue;
            for (const k of Object.keys(current)) {
                if (forbidden.has(k)) return true;
                const v = current[k];
                if (isPlainObject(v)) stack.push(v);
                else if (Array.isArray(v)) v.forEach(i => { if (isPlainObject(i)) stack.push(i); });
            }
        }
        return false;
    }

    // Helper: strictly validate and sanitize the name field using a whitelist approach
    function validateAndSanitizeName(name) {
        if (typeof name !== "string") return null;
        const trimmed = name.trim();
        if (trimmed.length === 0 || trimmed.length > 500) return null;

        // Allow letters, numbers, common punctuation and whitespace.
        // This reduces XSS risk by disallowing characters used in tags/JS injection.
        // Unicode letters/numbers are allowed via \p{L}\p{N} (requires Node with unicode property escapes support).
        const whitelist = /^[\p{L}\p{N}\s\-\_\.\,\'\"@()]{1,500}$/u;
        if (!whitelist.test(trimmed)) return null;

        // Return the trimmed value (JSON encoding by res.json is safe)
        return trimmed;
    }

    try {
        // Use a safe schema to avoid executing/custom types during deserialization.
        // JSON_SCHEMA restricts parsing to the JSON-compatible subset of YAML.
        const parsed = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

        if (!isPlainObject(parsed)) {
            return res.status(400).json({ error: "YAML must deserialize to a mapping/object." });
        }

        if (containsProtoPollutionKeys(parsed)) {
            return res.status(400).json({ error: "Payload contains forbidden keys." });
        }

        const rawName = parsed.name;
        const safeName = validateAndSanitizeName(rawName);
        if (safeName === null) {
            return res.status(400).json({ error: "Missing or invalid 'name' field." });
        }

        // Return JSON (safe against XSS when consumed as JSON). Do not leak internal errors.
        return res.json({ name: safeName });
    } catch (err) {
        // Generic error response to avoid leaking internal details
        return res.status(400).json({ error: "Unable to parse YAML payload." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
