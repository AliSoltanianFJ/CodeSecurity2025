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
  if (typeof yaml_payload !== "string") {
    return res.status(400).json({ error: "Missing or invalid 'payload' query parameter" });
  }

  try {
    // Only parse JSON-compatible YAML (no functions, no custom types, no anchors, etc).
    const doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });

    // Basic validation
    if (doc === null || typeof doc !== "object") {
      throw new Error("YAML did not deserialize to an object");
    }
    const name = doc.name;
    if (typeof name !== "string") {
      throw new Error("Missing or invalid 'name' field");
    }

    // Return just the name field as JSON
    res.json({ name });
  } catch (err) {
    // On parse error or validation error, return 400
    res.status(400).json({ error: "Invalid YAML payload" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
