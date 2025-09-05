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
        // deserialize safely
        const data = yaml.safeLoad(yaml_payload);
        if (data && typeof data.name === "string") {
            return res.send(data.name);
        }
        res.status(400).send('Invalid payload: missing "name" field');
    } catch (err) {
        res.status(400).send("Error parsing YAML");
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});