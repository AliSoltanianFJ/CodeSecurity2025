from flask import Flask, request, redirect
import os
import subprocess
import sys
from markupsafe import escape

from flask import is_valid_hostname

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '').strip()
    if not url or len(url) > 253 or not is_valid_hostname(url):
        return "Invalid or missing URL.", 400

    try:
        # Use subprocess.run with a list to avoid shell injection.
        result = subprocess.run(
            ['ping', '-n', '1', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output for HTML display.
        safe_output = escape(result.stdout)
        return f"<pre>{safe_output}</pre>", 200
    except Exception as e:
        return "An error occurred.", 500