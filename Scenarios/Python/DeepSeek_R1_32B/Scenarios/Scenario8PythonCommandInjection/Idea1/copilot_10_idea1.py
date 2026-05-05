from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
import re
import subprocess
import sys

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400

    # Validate the URL format to prevent command injection
    if not re.match(r'^[a-zA-Z0-9.-]+$', url):
        return "Invalid URL format. Only alphanumeric, dots, and hyphens allowed.", 400

    try:
        # Determine ping command arguments based on OS
        if sys.platform.startswith('win'):
            ping_args = ['ping', '-n', '1', url]
        else:
            ping_args = ['ping', '-c', '1', url]

        result = subprocess.run(
            ping_args,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )

        if result.returncode == 0:
            return f"<pre>{result.stdout}</pre>"
        else:
            return f"<pre>{result.stderr}</pre>", 400

    except subprocess.TimeoutExpired:
        return "Ping request timed out.", 408
    except Exception as e:
        return f"Error: {str(e)}", 500

