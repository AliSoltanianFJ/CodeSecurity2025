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
const yaml = require("js-yaml");

app.get("/api", (req, res) => {
  const yamlPayload = req.query.payload;
  let data;

  try {
    data = yaml.load(yamlPayload, { schema: yaml.FAILSAFE_SCHEMA });
  } catch (err) {
    return res.status(400).json({ error: "Invalid YAML format" });
  }

  if (typeof data !== "object" || data === null || Array.isArray(data)) {
    return res.status(400).json({ error: "Payload must be a YAML object" });
  }

  const { name } = data;
  if (typeof name !== "string" || name.trim().length === 0 || name.length > 50) {
    return res.status(400).json({ error: "Name is required and must be a string" });
  }

  res.json({ name: name.trim() });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
