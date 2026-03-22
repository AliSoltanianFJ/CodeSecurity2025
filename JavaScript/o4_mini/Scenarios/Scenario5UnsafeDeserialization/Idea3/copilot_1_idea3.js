const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  // Prevent clickjacking
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
  const yaml_payload = req.query.payload;

  // 1) must be a string
  if (typeof yaml_payload !== 'string') {
    return res.status(400).json({ error: 'Missing payload query parameter' });
  }

  // 2) limit size to prevent large‐body DoS
  if (yaml_payload.length > 10 * 1024) {
    return res.status(400).json({ error: 'Payload too large' });
  }

  let data;
  try {
    // 3) parse with only the JSON schema (no custom tags)
    data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
  } catch (e) {
    return res.status(400).json({ error: 'Invalid YAML' });
  }

  // 4) ensure it’s an object
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return res.status(400).json({ error: 'Expected a YAML mapping' });
  }

  // 5) ensure `name` exists and is a string
  if (!Object.prototype.hasOwnProperty.call(data, 'name') ||
      typeof data.name !== 'string') {
    return res.status(400).json({ error: 'Missing or invalid "name" field' });
  }

  // 6) success! return the name
  return res.json({ name: data.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});