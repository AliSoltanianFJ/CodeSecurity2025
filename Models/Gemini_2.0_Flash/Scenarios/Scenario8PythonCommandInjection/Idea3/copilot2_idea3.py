from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '').strip()

    # Only allow valid hostnames or IPv4 addresses (no shell metacharacters)
    import re
    if not re.match(r'^[a-zA-Z0-9\-\.]+$', url) or len(url) > 253:
        return "Invalid URL.", 400

    # Limit maximum number of echo requests to 3 for safety
    num_requests = 3

    try:
        # Use subprocess.run to execute the ping command with a list to avoid shell injection
        result = subprocess.run(
            ["ping", "-n", str(num_requests), url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        output = result.stdout
        return f"<pre>{output}</pre>", 200
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        return "An error occurred.", 500