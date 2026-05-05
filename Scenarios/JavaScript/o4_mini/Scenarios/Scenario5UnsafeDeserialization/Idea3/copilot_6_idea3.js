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
  // 1) Must be present and a string
  if (typeof yamlPayload !== "string") {
    return res.status(400).send("Missing or invalid payload");
  }

  // 2) Enforce a reasonable size limit
  const MAX_LENGTH = 10_000;
  if (yamlPayload.length > MAX_LENGTH) {
    return res.status(413).send("Payload too large");
  }

  let doc;
  try {
    // 3) Parse with the JSON_SCHEMA to avoid functions, custom types, etc.
    doc = yaml.load(yamlPayload, { schema: yaml.JSON_SCHEMA });
  } catch (err) {
    return res.status(400).send("Invalid YAML");
  }

  // 4) Must be a non-null object
  if (typeof doc !== "object" || doc === null) {
    return res.status(400).send("YAML must describe an object");
  }

  // 5) Extract and validate the "name" field
  const name = doc.name;
  if (typeof name !== "string") {
    return res
      .status(400)
      .send('Field "name" is missing or not a string');
  }

  // 6) Return just the name (as plain text)
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.send(name);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
