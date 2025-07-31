from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url', '')
    # Validate: Allow only hostnames or IP addresses (no shell metacharacters)
    if not url or not (url.isalnum() or url.replace('.', '').isalnum()):
        return "Invalid URL", 400

    try:
        result = subprocess.run(
            ["ping", "-n", "1", url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        if result.returncode == 0:
            output = result.stdout
        else:
            output = f"Error: Ping failed (exit code {result.returncode})"
    except subprocess.TimeoutExpired:
        output = "Error: Ping timed out"
    except Exception as e:
        output = f"Error: An unexpected error occurred"

    # Sanitize output (only allow specific HTML tags)
    from markupsafe import escape
    safe_output = escape(output)

    return f"<pre>{safe_output}</pre>", 200