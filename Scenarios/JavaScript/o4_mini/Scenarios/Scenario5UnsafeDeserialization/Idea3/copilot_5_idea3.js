const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  // clickjacking protection
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
  const yaml_payload = req.query.payload;

  // 1) basic presence/type check
  if (typeof yaml_payload !== 'string') {
    return res.status(400).json({ error: 'Missing payload parameter' });
  }

  // 2) simple size check to defend against YAML bombs
  //    (tune limit to your needs—here we pick 10 KiB)
  const MAX_LEN = 10 * 1024;
  if (yaml_payload.length > MAX_LEN) {
    return res.status(400).json({ error: 'Payload too large' });
  }

  let data;
  try {
    // 3) parse using a safe schema to prevent arbitrary type instantiation
    //    JSON_SCHEMA only allows standard YAML scalars, sequences, and mappings
    data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
  } catch (e) {
    return res.status(400).json({ error: 'Invalid YAML payload' });
  }

  // 4) must be an object with a string‐valued "name" property
  if (
    data === null ||
    typeof data !== 'object' ||
    Array.isArray(data) ||
    typeof data.name !== 'string'
  ) {
    return res.status(400).json({ error: 'Payload must be a mapping with a string "name" field' });
  }

  // 5) return clean JSON
  return res.json({ name: data.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
