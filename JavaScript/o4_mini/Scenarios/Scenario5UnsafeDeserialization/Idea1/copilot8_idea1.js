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

// …existing code…
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    try {
        // Use the JSON_SCHEMA to allow only plain objects, arrays, and scalars
        const data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        // Validate that we got an object with a string "name" property
        if (
            typeof data !== 'object' ||
            data === null ||
            typeof data.name !== 'string'
        ) {
            return res.status(400).send('Invalid payload: missing or invalid "name" field');
        }
        res.json({ name: data.name });
    } catch (err) {
        res.status(400).send('Invalid YAML payload');
    }
});
// …existing code…
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
