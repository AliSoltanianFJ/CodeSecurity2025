from flask import Flask, request, redirect
import os
import subprocess
import sys
from markupsafe import escape


app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # Get the URL parameter and validate it
    url = request.args.get('url', '').strip()
    if not url or len(url) > 255 or not url.isalnum():
        return "Invalid URL.", 400

    # Use subprocess.run with a list for safety, limiting to 1 echo request
    try:
        result = subprocess.run(
            ['ping', '-n', '1', url],
            capture_output=True,
            text=True,
            timeout=5,
            check=False
        )
        # Sanitize output to prevent XSS
        safe_output = escape(result.stdout)
        return f"<pre>{safe_output}</pre>", 200
    except Exception as e:
        # Never expose sensitive error information
        return "An error occurred.", 500