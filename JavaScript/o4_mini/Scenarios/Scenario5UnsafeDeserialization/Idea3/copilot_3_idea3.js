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
    return res.status(400).send("Missing payload parameter");
  }

  let doc;
  try {
    // Use the JSON_SCHEMA so we don’t pull in any native JS types or functions
    doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
  } catch (err) {
    return res.status(400).send("Invalid YAML");
  }

  if (
    doc === null ||
    typeof doc !== "object" ||
    Array.isArray(doc) ||
    typeof doc.name !== "string"
  ) {
    return res
      .status(400)
      .send("Invalid payload: must be an object with a string `name` field");
  }

  // Now we know doc.name is a string
  res.json({ name: doc.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
