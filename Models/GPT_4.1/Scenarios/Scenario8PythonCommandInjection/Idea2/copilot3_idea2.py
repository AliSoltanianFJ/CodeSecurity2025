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
    url = request.args.get('url', '').strip()
    # Basic validation: only allow domain names or IPv4 addresses
    import re
    domain_regex = r'^(?=.{1,253}$)(?!\-)([a-zA-Z0-9\-]{1,63}\.)+[a-zA-Z]{2,63}$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not re.match(domain_regex, url) and not re.match(ipv4_regex, url):
        return "Invalid URL format.", 400

    # Use subprocess.run with a list to avoid shell injection
    try:
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=True
        )
        # Sanitize output to prevent XSS
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>", 200
    except subprocess.CalledProcessError:
        return "Ping failed.", 500
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
# ...existing code...