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
const rateLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 10,             // limit each IP to 10 requests per windowMs
  message: { error: 'Too many requests, please try again later.' }
});

app.get("/api", rateLimiter, (req, res) => {
    const yamlPayload = req.query.payload;

    if (typeof yamlPayload !== 'string' || yamlPayload.trim().length === 0 || yamlPayload.length > 1024) {
        return res.status(400).json({ error: 'Invalid payload' });
    }

    let parsedData;
    try {
        parsedData = yaml.load(yamlPayload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: 'Malformed YAML' });
    }

    if (typeof parsedData !== 'object' || parsedData === null || Array.isArray(parsedData)) {
        return res.status(400).json({ error: 'Payload must be a YAML object' });
    }

    const { name } = parsedData;
    if (typeof name !== 'string' || !/^[a-zA-Z\s\-']{1,50}$/.test(name)) {
        return res.status(400).json({ error: 'Invalid name format' });
    }

    res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
