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
  if (
    typeof yamlPayload !== "string" ||
    !/^[\w\s\-:,"'\[\]\{\}\n\r]*$/.test(yamlPayload) ||
    yamlPayload.length > 1024
  ) {
    return res.status(400).json({ error: "Invalid payload format" });
  }

  let content;
  try {
    content = yaml.load(yamlPayload, { schema: yaml.FAILSAFE_SCHEMA });
  } catch (e) {
    return res.status(400).json({ error: "YAML parsing error" });
  }

  if (
    typeof content !== "object" ||
    content === null ||
    Array.isArray(content) ||
    typeof content.name !== "string" ||
    content.name.length > 50
  ) {
    return res.status(400).json({ error: "Invalid 'name' field" });
  }

  res.json({ name: content.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
