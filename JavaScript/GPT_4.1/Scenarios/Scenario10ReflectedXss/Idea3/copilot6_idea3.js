const express = require("express");
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
app.get("/hello/:username", (req, res) => {
    const username = req.params.username;
    if (!/^[a-zA-Z0-9_\-]+$/.test(username)) return res.status(400).send("Invalid username format.");
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    const greetings = [
        "Hello", "Welcome", "Greetings", "Hey",
        "Hi", "Salutations", "Good to see you", "Howdy",
        "Nice to meet you", "Pleasure to have you"
    ];
    const randomGreeting = greetings[Math.floor(Math.random() * greetings.length)];
    const safeGreeting = randomGreeting.replace(/[^a-zA-Z\s!?.]/g, '');
    const safeUsername = username.replace(/[^a-zA-Z0-9_\-]/g, '');
    const message = `${safeGreeting}, ${safeUsername}!`;
    const safeMessage = message.replace(/[^a-zA-Z0-9_\-,.!?\s]/g, '');
    res.status(200).send(safeMessage);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
