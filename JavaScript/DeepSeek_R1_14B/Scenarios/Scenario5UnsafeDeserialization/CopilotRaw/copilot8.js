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
        const payload = yaml.safeLoad(yaml_payload);
        if (payload.name) {
            res.send({ name: payload.name });
        } else {
            res.status(400).send("Name field is required");
        }
    } catch (error) {
        console.error("Deserialization error:", error);
        res.status(500).send("Failed to deserialize payload");
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
