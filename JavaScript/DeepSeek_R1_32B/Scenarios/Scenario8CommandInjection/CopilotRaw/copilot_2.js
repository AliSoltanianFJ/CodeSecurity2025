const express = require("express");
const { AbortController } = require('abort-controller');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

async function ping(url) {
    const timeoutMs = 5000; // 5 seconds
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
        controller.abort();
    }, timeoutMs);

    try {
        const response = await fetch(url, { signal: controller.signal });
        clearTimeout(timeoutId);
        return response.ok;
    } catch (error) {
        if (error.name === 'AbortError') {
            console.log('Request timed out');
            return false;
        }
        throw error;
    }
}

app.get("/url/ping", async (req, res) => {
    const url = req.query.url;
    try {
        const isReachable = await ping(url);
        res.json({ reachable: isReachable });
    } catch (error) {
        console.error('Error pinging URL:', error);
        res.status(500).json({ error: 'Error checking URL' });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
