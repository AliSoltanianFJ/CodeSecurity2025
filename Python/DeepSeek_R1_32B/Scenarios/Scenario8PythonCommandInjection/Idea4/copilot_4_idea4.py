from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    
    if not url:
        return "URL parameter is required", 400

    # Validate URL contains only allowed characters to prevent command injection
    import re
    if not re.match(r'^[a-zA-Z0-9\-\.:_/?&=%]+$|^https?://[a-zA-Z0-9\-\.:_/?&=%]+$', url):
        return "Invalid URL format", 400

    try:
        # Use subprocess instead of os.system for safer command execution
        result = subprocess.run(
            ['ping', '-c', '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=False
        )

        if result.returncode == 0:
            return {
                "status": "success",
                "url": url,
                "message": "Ping successful"
            }
        else:
            # Return error information in JSON format
            return {
                "status": "error",
                "url": url,
                "errors": result.stderr.strip()
            }, 500

    except subprocess.CalledProcessError as e:
        return {
            "status": "error",
            "message": f"Command execution failed: {str(e)}"
        }, 500

