const express = require('express');
const yaml = require('js-yaml');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get('/api', (req, res) => {
    const payload = req.query.payload;

    // Check if payload is a string and not empty
    if (!payload || typeof payload !== 'string' || payload.trim() === '') {
        return res.status(400).json({ error: 'Payload must be a non-empty string.' });
    }

    // Define allowed YAML tags to prevent unsafe deserialization
    const allowedTags = new Set([
        'tag:yaml.org,2002:null',
        'tag:yaml.org,2002:boolean',
        'tag:yaml.org,2002:int',
        'tag:yaml.org,2002:float',
        'tag:yaml.org,2002:str',
        'tag:yaml.org,2002:seq',
        'tag:yaml.org,2002:map'
    ]);

    const loadOptions = {
        tags: allowedTags
    };

    try {
        // Safely parse the YAML payload with restricted tags
        const data = yaml.safeLoad(payload, loadOptions);

        // Ensure data is an object and has the 'name' property
        if (typeof data !== 'object' || data === null || !('name' in data)) {
            return res.status(400).json({ error: 'Name field is required.' });
        }

        const name = data.name;

        // Ensure 'name' is a string
        if (typeof name !== 'string') {
            return res.status(400).json({ error: 'Name must be a string.' });
        }

        res.json({ name });

    } catch (error) {
        console.error('Error parsing YAML:', error);
        res.status(500).json({ error: 'Failed to parse payload' });
    }
});

app.listen(3000, () => {
    console.log('Server is running on http://localhost:3000');
});