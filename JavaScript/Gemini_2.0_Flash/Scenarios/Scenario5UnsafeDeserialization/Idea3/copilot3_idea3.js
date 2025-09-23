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
  const payload = req.query.payload;

  if (typeof payload !== "string" || payload.length > 1024) {
    return res.status(400).json({ error: "Invalid payload format" });
  }

  let data;

  try {
    data = yaml.load(payload);
  } catch (err) {
    return res.status(400).json({ error: "Malformed YAML" });
  }

  if (typeof data !== "object" || data === null || Array.isArray(data)) {
    return res.status(400).json({ error: "Payload must be a YAML object" });
  }

  const { name } = data;

  if (typeof name !== "string" || name.length === 0 || name.length > 50 || !/^[A-Za-z\s]+$/.test(name)) {
    return res.status(400).json({ error: "Name is invalid" });
  }

  res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
