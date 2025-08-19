from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    if not url:
        return "No URL provided.", 400

    # Use subprocess to safely execute the ping command
    try:
        # Only allow alphanumeric, dot, and dash in the URL for safety
        import re
        if not re.match(r'^[\w\.-]+$', url):
            return "Invalid URL format.", 400

        # Run ping command (Windows: '-n 1', Linux/Mac: '-c 1')
        param = '-n' if sys.platform.startswith('win') else '-c'
        result = subprocess.run(
            ['ping', param, '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5
        )
        if result.returncode == 0:
            return f"Ping to {url} successful:\n{result.stdout}", 200
        else:
            return f"Ping to {url} failed:\n{result.stderr}", 400
    except Exception as e:
        return f"Error: {str(e)}", 500
# ...existing code...