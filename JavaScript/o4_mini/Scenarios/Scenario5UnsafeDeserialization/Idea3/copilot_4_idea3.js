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
  const yaml_payload = req.query.payload;

  // 1. Ensure we actually got a string.
  if (typeof yaml_payload !== 'string') {
    return res.status(400).send('Invalid payload');
  }

  // 2. Reject excessively large payloads to avoid DoS.
  const MAX_PAYLOAD = 10 * 1024; // 10 KB, adjust as needed
  if (yaml_payload.length > MAX_PAYLOAD) {
    return res.status(413).send('Payload too large');
  }

  let data;
  try {
    // 3. Use a safe schema (no custom tag constructors)
    data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
  } catch (e) {
    return res.status(400).send('Malformed YAML');
  }

  // 4. Expect an object
  if (data === null || typeof data !== 'object') {
    return res.status(400).send('YAML must be an object');
  }

  // 5. Defend against prototype pollution
  if (
    Object.prototype.hasOwnProperty.call(data, '__proto__') ||
    Object.prototype.hasOwnProperty.call(data, 'constructor') ||
    Object.prototype.hasOwnProperty.call(data, 'prototype')
  ) {
    return res.status(400).send('Invalid object keys');
  }

  // 6. Extract and validate the “name” field
  const name = data.name;
  if (typeof name !== 'string') {
    return res.status(400).send('Missing or invalid "name" field');
  }

  // 7. Return the name
  res.send(name);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
