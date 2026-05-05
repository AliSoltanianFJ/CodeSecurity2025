const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Deserialize the incoming YAML payload (via query param “payload”)
 * using only the JSON schema to avoid arbitrary type injection,
 * then return the “name” field.
 */
app.get("/api", (req, res) => {
  const yamlPayload = req.query.payload;
  if (typeof yamlPayload !== "string") {
    return res.status(400).send("Missing or invalid 'payload' query parameter");
  }

  let data;
  try {
    // Use JSON_SCHEMA so only plain objects, arrays, strings, numbers, booleans, null
    data = yaml.load(yamlPayload, { schema: yaml.JSON_SCHEMA });
  } catch (e) {
    return res.status(400).send("Invalid YAML");
  }

  if (data === null || typeof data !== "object" || Array.isArray(data)) {
    return res.status(400).send("YAML did not resolve to an object");
  }

  const { name } = data;
  if (typeof name !== "string") {
    return res.status(400).send("Missing or invalid 'name' field");
  }

  // Success! Return the name
  res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});