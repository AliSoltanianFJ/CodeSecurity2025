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
    if (!yaml_payload || typeof yaml_payload !== "string" || yaml_payload.length > 1024 || !/^[\w\s\-:,"'\[\]\{\}\n\r]+$/.test(yaml_payload)) return res.status(400).send("Invalid payload");
    let data;
    try { data = yaml.safeLoad(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA }); }
    catch (e) { return res.status(400).send("YAML parsing error"); }
    if (!data || typeof data !== "object" || Array.isArray(data) || typeof data.name !== "string" || !/^[A-Za-z\s\-]+$/.test(data.name) || data.name.length > 50) return res.status(400).send("Invalid name property");
    res.json({ name: data.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
