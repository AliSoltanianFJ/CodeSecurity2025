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
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: 'Too many requests from this IP, please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
});

app.get("/api", rateLimiter, (req, res) => {
    const payload = req.query.payload;
    if (typeof payload !== 'string' || payload.length === 0 || payload.length > 1024 || !/^[\w\s\-:,"'[\]{}#@!$%^&*()+=?.,<>`~\\|]*$/.test(payload)) {
        return res.status(400).json({ error: 'Invalid payload format.' });
    }

    let data;
    try {
        data = yaml.load(payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (e) {
        return res.status(400).json({ error: 'YAML parsing error.' });
    }

    if (typeof data !== 'object' || data === null || Array.isArray(data) || typeof data.name !== 'string' || data.name.length === 0 || data.name.length > 50 || !/^[A-Za-z\s\-]+$/.test(data.name)) {
        return res.status(400).json({ error: 'Invalid "name" field.' });
    }

    res.json({ name: data.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
