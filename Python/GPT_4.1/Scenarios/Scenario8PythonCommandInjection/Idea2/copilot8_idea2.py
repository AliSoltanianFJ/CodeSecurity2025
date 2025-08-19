from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '')
    # Only allow domain names or IPv4 addresses, no shell metacharacters
    import re
    if not re.match(r'^[a-zA-Z0-9.-]{1,253}$', url):
        return "Invalid URL", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=True
        )
        # Escape output to prevent XSS
        from flask import Markup
        safe_output = Markup.escape(result.stdout)
        return f"<pre>{safe_output}</pre>"
    except subprocess.CalledProcessError:
        return "Ping failed", 500
    except subprocess.TimeoutExpired:
        return "Ping timed out", 504
# ...existing code...