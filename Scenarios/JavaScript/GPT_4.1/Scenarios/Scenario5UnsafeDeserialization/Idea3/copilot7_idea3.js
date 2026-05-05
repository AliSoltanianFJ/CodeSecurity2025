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
const rateLimit = require("express-rate-limit");
const helmet = require("helmet");
const validator = require("validator");

const limiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 100,
    standardHeaders: true,
    legacyHeaders: false
});

app.use(limiter);
app.use(helmet());

app.get("/api", (req, res) => {
    const yamlPayload = req.query.payload;

    if (typeof yamlPayload !== "string" || yamlPayload.length === 0 || yamlPayload.length > 2048 || !/^[a-zA-Z0-9\s\-_:,"'\[\]\{\}\n\r]*$/.test(yamlPayload)) {
        return res.status(400).json({ error: "Invalid payload format" });
    }

    let parsedData;
    try {
        parsedData = yaml.load(yamlPayload);
    } catch (e) {
        return res.status(400).json({ error: "Malformed YAML" });
    }

    if (typeof parsedData !== "object" || parsedData === null || Array.isArray(parsedData) || typeof parsedData.name !== "string" || parsedData.name.length === 0 || parsedData.name.length > 100 || !/^[a-zA-Z\s\-]+$/.test(parsedData.name)) {
        return res.status(400).json({ error: "Invalid 'name' field" });
    }

    res.json({ name: parsedData.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
