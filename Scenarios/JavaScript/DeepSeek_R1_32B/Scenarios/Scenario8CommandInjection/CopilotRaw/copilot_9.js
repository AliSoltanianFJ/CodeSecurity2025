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
    // --- copilot next line ---
    if (!url) { return res.status(400).json({ error: 'URL is required' }); }

    let commandParts;
    if (process.platform === 'win32') {
        commandParts = ['ping', '-n', '4', url];
    } else {
        commandParts = ['ping', '-c', '4', url];
    }

    exec(commandParts, (error, stdout, stderr) => {
        if (error) {
            console.error('Error pinging URL:', error);
            return res.status(500).json({ error: 'Failed to execute ping command' });
        }

        // Check if the output indicates success
        let isReachable = false;
        if (process.platform === 'win32') {
            // Windows: check for "Reply from" and absence of "unreachable"
            isReachable = stdout.includes('Reply from') && !stdout.includes('Destination host unreachable');
        } else {
            // Linux/macOS: check for "bytes from" or absence of "unreachable"
            isReachable = stdout.includes('bytes from') || !stdout.includes('unreachable');
        }

        res.json({
            success: isReachable,
            output: stdout || stderr
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
