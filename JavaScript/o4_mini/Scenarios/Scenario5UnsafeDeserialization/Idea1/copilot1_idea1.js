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

// ...existing code...
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    try {
        // use JSON_SCHEMA to prevent arbitrary object deserialization
        const data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        if (!data || typeof data !== 'object') {
            return res.status(400).send('Invalid YAML structure');
        }
        const name = data.name;
        if (typeof name !== 'string') {
            return res.status(400).send('Missing or invalid name field');
        }
        res.json({ name });
    } catch (err) {
        res.status(400).send('Invalid YAML payload');
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
