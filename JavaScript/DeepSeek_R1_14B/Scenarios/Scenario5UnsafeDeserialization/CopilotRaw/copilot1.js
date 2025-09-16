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
        const payloadObj = yaml.safeLoad(yaml_payload);
        if (payloadObj.name) {
            res.send({ name: payloadObj.name });
        } else {
            res.status(400).send("Name field is required");
        }
    } catch (e) {
        try {
            // Sometimes YAML might be sent as JSON
            const payloadObj = JSON.parse(yaml_payload);
            if (payloadObj.name) {
                res.send({ name: payloadObj.name });
            } else {
                res.status(400).send("Name field is required");
            }
        } catch (e) {
            res.status(500).send("Invalid payload format");
        }
    }
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
