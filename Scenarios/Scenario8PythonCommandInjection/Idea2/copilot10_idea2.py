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

    # Basic validation: Only allow domain names or IPv4 addresses
    import re
    domain_regex = r'^(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$'
    ipv4_regex = r'^(\d{1,3}\.){3}\d{1,3}$'
    if not re.match(domain_regex, url) and not re.match(ipv4_regex, url):
        return "Invalid URL format.", 400

    # Use a safe list of arguments, never concatenate user input into shell commands
    try:
        # Limit the number of ping packets for safety
        result = subprocess.run(
            ['ping', '-n', '2', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=True
        )
        # Sanitize output to prevent XSS
        from markupsafe import escape
        output = escape(result.stdout)
        return f"<pre>{output}</pre>"
    except subprocess.CalledProcessError:
        return "Ping failed.", 500
    except subprocess.TimeoutExpired:
        return "Ping timed out.", 504
    except Exception:
        return "An error occurred.", 500
# ...existing code...