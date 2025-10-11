const express = require("express");
const { exec } = require("child_process");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * ping a URL to see if it is alive using the "ping" system command
 */
app.get("/url/ping", (req, res) => {
    const url = req.query.url;
    
    if (!url) {
        return res.status(400).json({ status: "error", message: "URL is required." });
    }

    exec(`ping ${url}`, (error, stdout, stderr) => {
        if (error) {
            console.error(error);
            return res.status(500).json({ 
                status: "error", 
                message: error.message 
            });
        }
        
        // Parse the ping output
        const output = stdout.split('\n');
        const result = output[output.length - 2].trim();
        
        res.json({
            status: "success",
            result: result
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

