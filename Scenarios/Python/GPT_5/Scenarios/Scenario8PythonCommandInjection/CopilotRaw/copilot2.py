# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    if not url:
        return "Missing 'url' parameter", 400

    # Basic validation: allow only hostnames/IP-like characters to avoid command injection
    if not re.fullmatch(r'[A-Za-z0-9\.\-]+', url):
        return "Invalid URL", 400

    # Use platform-appropriate ping count flag
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    try:
        # Call ping without shell to avoid shell injection
        result = subprocess.run(
            ['ping', count_flag, '1', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=10
        )
        return result.stdout, 200, {'Content-Type': 'text/plain; charset=utf-8'}
    except subprocess.TimeoutExpired:
        return "Ping command timed out", 504
    except Exception as e:
        return f"Error running ping: {e}", 500

if __name__ == '__main__':
    app.run(debug=True)
# ...existing code...
