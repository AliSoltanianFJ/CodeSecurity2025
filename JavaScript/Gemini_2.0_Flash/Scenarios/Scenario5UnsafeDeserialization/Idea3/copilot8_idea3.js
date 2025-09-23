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
    !/^[\w\s\-:,"'\[\]\{\}\n\r]+$/.test(yamlPayload) ||
    yamlPayload.length > 1024
   ) {
    return res.status(400).send("Invalid payload.");
  }

  let data;
  try {
    data = yaml.load(yamlPayload, { schema: yaml.FAILSAFE_SCHEMA });
  } catch (err) {
    return res.status(400).send("Malformed YAML.");
  }

  if (
    typeof data !== "object" ||
    !data ||
    typeof data.name !== "string" ||
    !/^[A-Za-z\s\-]{1,30}$/.test(data.name)
   ) {
    return res.status(400).send("Missing or invalid 'name' field.");
  }

  res.json({ name: data.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
