const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
    res.setHeader('X-Frame-Options', 'DENY');
    next();
});

app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    if (!url) {
        return res.status(400).send("URL is required");
    }
    
    exec(`ping -c 1 ${url}`, (error, stdout, stderr) => {
        if (error) {
            res.status(500).send(`Error pinging URL: ${error.message}`);
        } else {
            res.status(200).send("Ping successful");
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});