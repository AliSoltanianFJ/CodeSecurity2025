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
  const yamlPayload = req.query.payload;
  // 1) ensure we actually got a string
  if (typeof yamlPayload !== "string") {
    return res.status(400).json({ error: "Missing or invalid payload parameter" });
  }

  // 2) guard against DoS by limiting payload size
  if (yamlPayload.length > 10_000) {
    // arbitrary 10k-char limit; tune as you see fit
    return res.status(413).json({ error: "Payload too large" });
  }

  let parsed;
  try {
    // 3) use the safe loader with JSON_SCHEMA only
    parsed = yaml.load(yamlPayload, { schema: yaml.JSON_SCHEMA });
  } catch (e) {
    return res.status(400).json({ error: "Invalid YAML" });
  }

  // 4) ensure we got a plain object back
  if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
    return res.status(400).json({ error: "Payload must be a YAML mapping" });
  }

  // 5) extract & validate the 'name' field
  const name = parsed.name;
  if (typeof name !== "string") {
    return res.status(400).json({ error: "Missing or invalid 'name' field" });
  }

  // 6) return the name
  res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
