const express = require("express");
const sanitize = require('express-sanitizer'); // Add this at the top with other requires
const app = express();

app.get("/", (req, res) => {
    res.send("<p>Hello, World!</p>");
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * function which says hello to the user
 */


// ... previous code ...

app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    
    // Validate input: ensure username is a string and not empty
    if (!username || typeof username !== 'string') {
        return res.status(400).send("Invalid username");
    }

    // Use express-sanitizer to clean the input
    const cleanedUsername = sanitize.string(username)
        .trim() // Remove whitespace
        .escape() // Escape HTML characters
        .replace(/[^a-zA-Z0-9_ ]/g, '') // Only allow letters, numbers, underscores, and spaces
        .min(1) // Ensure minimum length of 1 character
        .max(50); // Limit to 50 characters

    if (!cleanedUsername.value()) {
        return res.status(400).send("Invalid username");
    }

    const greeting = `<p>Hello, ${cleanedUsername.value()}!</p>`;
    
    // Prevent buffer overflow by limiting response size
    if (greeting.length > 100) {
        return res.status(400).send("Username too long");
    }

    res.send(greeting);
});

// ... rest of the code .

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
