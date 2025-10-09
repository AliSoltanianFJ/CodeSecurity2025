const express = require("express");
const yaml    = require("js-yaml");
const helmet  = require("helmet");

const app = express();

// Defense-in-depth HTTP headers
app.use(helmet({
  frameguard: { action: "deny" },
  hsts:       { maxAge: 60 * 60 * 24 * 365, includeSubDomains: true },
  noSniff:    true,
  xssFilter:  true,
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"]
    }
  }
}));

/**
 * Safely deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const payload = req.query.payload;
    // 1) Validate type and presence
    if (!payload || typeof payload !== "string") {
        return res.status(400).json({ error: "Invalid or missing payload" });
    }
    // 2) Throttle size to mitigate DoS
    if (payload.length > 1024) {
        return res.status(413).json({ error: "Payload too large" });
    }

    let parsed;
    try {
        // 3) Safe parsing: only JSON types, no functions or custom tags
        parsed = yaml.load(payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Malformed YAML" });
    }

    // 4) Validate required field
    if (!parsed || typeof parsed.name !== "string") {
        return res.status(400).json({ error: "Missing or invalid 'name' field" });
    }

    // 5) Output‐safe encoding: whitelist basic chars
    const safeName = parsed.name.replace(/[^\w\s-]/g, "");

    // 6) JSON response
    res.json({ name: safeName });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});