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
  const yamlInput = req.query.payload;

  if (typeof yamlInput !== "string" || yamlInput.length > 1024) {
    return res.status(400).json({ error: "Payload is invalid" });
  }

  let parsedData;
  try {
    parsedData = yaml.load(yamlInput, { schema: yaml.FAILSAFE_SCHEMA });
  } catch (error) {
    return res.status(400).json({ error: "Malformed YAML" });
  }

  if (
    typeof parsedData !== "object" ||
    parsedData === null ||
    Array.isArray(parsedData) ||
    typeof parsedData.name !== "string" ||
    parsedData.name.length > 100
  ) {
    return res.status(400).json({ error: "Payload is invalid" });
  }

  res.json({ name: parsedData.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
