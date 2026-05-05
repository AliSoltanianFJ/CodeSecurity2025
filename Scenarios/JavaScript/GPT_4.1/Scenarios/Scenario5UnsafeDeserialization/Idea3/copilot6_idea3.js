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
const rateLimit = require('express-rate-limit');

const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  standardHeaders: true,
  legacyHeaders: false,
});

const SAFE_YAML_REGEX = /^[a-zA-Z0-9\-_\s:,"'\[\]\{\}\n\r]*$/;

app.get("/api", apiLimiter, (req, res) => {
  const yamlPayload = req.query.payload;

  if (!yamlPayload || typeof yamlPayload !== 'string' || yamlPayload.length > 1024 || !SAFE_YAML_REGEX.test(yamlPayload)) {
    return res.status(400).json({ error: 'Invalid payload' });
  }

  let parsedData;
  try {
    parsedData = yaml.load(yamlPayload, { schema: yaml.FAILSAFE_SCHEMA });
  } catch (err) {
    return res.status(400).json({ error: 'YAML parsing error' });
  }

  if (typeof parsedData !== 'object' || Array.isArray(parsedData) || parsedData === null || typeof parsedData.name !== 'string' || parsedData.name.length > 100 || !/^[a-zA-Z\s\-]+$/.test(parsedData.name)) {
    return res.status(400).json({ error: 'Invalid "name" field' });
  }

  res.json({ name: parsedData.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
