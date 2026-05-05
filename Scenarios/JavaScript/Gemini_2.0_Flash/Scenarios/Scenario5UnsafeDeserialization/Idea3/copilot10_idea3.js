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

  if (typeof yamlPayload !== "string" || yamlPayload.trim().length === 0 || yamlPayload.length > 1024) {
    return res.status(400).json({ error: "Payload is invalid" });
  }

  try {
    const parsedData = yaml.load(yamlPayload, { schema: yaml.FAILSAFE_SCHEMA });

    if (typeof parsedData !== "object" || parsedData === null || Array.isArray(parsedData)) {
      return res.status(400).json({ error: "Payload is invalid YAML object" });
    }

    if (typeof parsedData.name !== "string" || parsedData.name.trim().length === 0 || parsedData.name.length > 100) {
      return res.status(400).json({ error: "Name property is invalid" });
    }

    res.json({ name: parsedData.name.trim() });
  } catch (err) {
    console.error("YAML parsing error:", err);
    res.status(400).json({ error: "Malformed YAML" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
