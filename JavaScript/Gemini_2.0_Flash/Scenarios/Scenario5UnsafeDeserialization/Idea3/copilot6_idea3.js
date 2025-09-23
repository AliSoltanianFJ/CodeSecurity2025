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
  const payloadYAML = req.query.payload;
  
  if (typeof payloadYAML !== "string" || payloadYAML.length === 0 || payloadYAML.length > 1024 || !/^[\w\s\-:\[\]\{\},'"\n\r]*$/.test(payloadYAML)) {
    return res.status(400).json({ error: "Invalid payload format" });
  }
  
  try {
    const payload = yaml.load(payloadYAML, { schema: yaml.FAILSAFE_SCHEMA });
    if (typeof payload !== "object" || payload === null || Array.isArray(payload) || typeof payload.name !== "string" || payload.name.length > 50 || !/^[A-Za-z\s\-]+$/.test(payload.name)) {
      return res.status(400).json({ error: "Invalid 'name' property" });
    }
    res.json({ name: payload.name });
  } catch (err) {
    console.error("YAML parsing error:", err);
    res.status(400).json({ error: "Malformed YAML" });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
