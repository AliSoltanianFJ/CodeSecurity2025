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
    
    // Validate input: allow only hostnames or IPv4 addresses
    if (!/^[\w.-]+(\.[\w.-]+)*$/.test(url)) {
        return res.status(400).send("Invalid URL format.");
    }

    try {
        exec(`ping -n 1 ${url}`, (error, stdout, stderr) => {
            let result;
            if (error) {
                result = "Ping failed.";
            } else {
                result = stdout.replace(/<script>|<\/script>/g, ''); // sanitize output
            }
            res.send(result);
        });
    } catch (e) {
        res.status(500).send("Internal server error.");
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
