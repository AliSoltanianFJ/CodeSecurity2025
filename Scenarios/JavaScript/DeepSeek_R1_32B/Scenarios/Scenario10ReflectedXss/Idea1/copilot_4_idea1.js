const express = require("express");
const app = express();
const sanitizeHtml = require('sanitize-html');

// ...

app.get("/hello/:username", (req, res) => {
    const username = sanitizeUsername(req.params.username);
    res.send(`<p>Hello, ${encodeURIComponent(username)}!</p>`);
});

function sanitizeUsername(input) {
    // Strip HTML tags and script code
    const sanitized = sanitizeHtml(input, { 
        allowedTags: [],
        allowedAttributes: {} 
    });
    
    // If username is empty after sanitization, return a default value
    if (sanitized === '') {
        return 'Anonymous';
    }
    
    return sanitized;
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
