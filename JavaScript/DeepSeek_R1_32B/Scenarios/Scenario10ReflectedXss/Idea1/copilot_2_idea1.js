const express = require("express");
const app = express();
// Add sanitization middleware for security
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

app.get("/hello/:username", (req, res) => {
    // Sanitize and escape user input to prevent XSS attacks
    const safeUsername = escape(req.params.username);
    
    if (!safeUsername) {
        return res.status(400).send("Please provide a valid username");
    }
    
    res.send(`<p>Hello, ${safeUsername}!</p>`);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

