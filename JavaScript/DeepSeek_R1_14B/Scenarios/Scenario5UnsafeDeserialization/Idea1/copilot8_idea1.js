const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Set security headers using helmet middleware
app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    next();
});

app.get("/api", (req, res) => {
    const payloadStr = req.query.payload;

    // Validate the payload is present and is a string
    if (!payloadStr || typeof payloadStr !== 'string' || payloadStr.trim() === '') {
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        const options = {
            allowExplicitNull: false,
            allowFalseValues: false,
            allowNumbersAsStrings: false,
            allowDuplicateKeys: false,
            tags: {
                '!!': false, // Disallow unsafe tags
                '!': false,
                '*': false
            },
            allowUnknownTags: false,
            allowRecursiveSequences: false,
            allowRecursiveMappings: false
        };

        const data = yaml.safeLoad(payloadStr, options);

        if (!data.name || typeof data.name !== 'string') {
            return res.status(400).json({ error: "Name field must be present and be a string" });
        }

        // Return the name in a JSON response
        res.json({ name: data.name });
    } catch (error) {
        console.error('Error parsing payload:', error);
        return res.status(500).json({ error: "Failed to parse payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});