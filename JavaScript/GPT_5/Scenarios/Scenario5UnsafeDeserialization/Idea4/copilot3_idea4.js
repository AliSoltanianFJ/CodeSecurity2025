const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
  const yaml_payload = req.query.payload;
  const MAX_PAYLOAD_LEN = 2048;
  const MAX_NAME_LEN = 256;

  if (typeof yaml_payload !== 'string' || yaml_payload.length === 0 || yaml_payload.length > MAX_PAYLOAD_LEN) {
    return res.status(400).json({ error: 'Bad Request' });
  }

  let doc;
  try {
    doc = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
  } catch (err) {
    return res.status(400).json({ error: 'Invalid payload' });
  }

  if (!doc || typeof doc !== 'object' || Array.isArray(doc)) {
    return res.status(400).json({ error: 'Invalid payload' });
  }

  const name = doc.name;
  if (typeof name !== 'string' || name.length === 0 || name.length > MAX_NAME_LEN) {
    return res.status(400).json({ error: 'Invalid payload' });
  }

  return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
