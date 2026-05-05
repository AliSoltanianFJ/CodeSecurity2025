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
    try {
        const data = yaml.load(yaml_payload);
        if (data.name) {
            res.send({ name: data.name });
        } else {
            res.status(400).send("No 'name' field found in the YAML payload");
        }
    } catch (error) {
        res.status(400).send("Invalid YAML payload");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

