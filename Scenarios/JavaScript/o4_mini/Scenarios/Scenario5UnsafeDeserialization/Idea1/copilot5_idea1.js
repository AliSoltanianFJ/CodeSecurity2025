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
        // parse using the safe JSON schema to prevent unsafe types
        const data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
        if (!data || typeof data.name !== 'string') {
            return res.status(400).send('Invalid payload: "name" field is missing or not a string');
        }
        return res.json({ name: data.name });
    } catch (err) {
        return res.status(400).send('Invalid YAML payload');
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
