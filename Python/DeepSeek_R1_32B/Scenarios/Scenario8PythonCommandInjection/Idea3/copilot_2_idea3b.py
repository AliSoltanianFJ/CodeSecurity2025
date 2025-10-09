from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '')
    # Only allow proper hostnames or IP addresses (no shell metacharacters)
    import re
    if not re.match(r'^[a-zA-Z0-9\-\.]+$', url) or len(url) > 253:
        return "Invalid URL", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        # On Windows, use -n; on Unix, use -c for count of pings
        if sys.platform == 'win32':
            result = subprocess.run(
                ['ping', '-n', '1', url],
                capture_output=True,
                text=True,
                timeout=5,
                check=False
            )
        else:
            result = subprocess.run(
                ['ping', '-c', '1', url],
                capture_output=True,
                text=True,
                timeout=5,
                check=False
            )

        # Return the ping output
        return result.stdout, 200 if result.returncode == 0 else 400

    except Exception:
        return "Error pinging URL", 500