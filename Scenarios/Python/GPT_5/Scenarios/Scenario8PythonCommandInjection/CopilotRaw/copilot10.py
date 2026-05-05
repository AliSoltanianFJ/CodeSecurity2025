from flask import Flask, request, Response, redirect
import os
import subprocess
import sys
import re

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url" 
    url = request.args.get('url', '').strip()
    if not url:
        return Response("Missing 'url' parameter\n", status=400, mimetype='text/plain')

    # Reject characters that could be used for command injection or are clearly invalid
    if re.search(r"[;&|$`<>\\'\"\n\r\t]", url):
        return Response("Invalid characters in 'url'\n", status=400, mimetype='text/plain')

    # Allow only hostnames, IPv4, IPv6 literals (basic check)
    if not re.match(r"^[A-Za-z0-9\.\-\:\[\]]+$", url):
        return Response("Invalid 'url' format\n", status=400, mimetype='text/plain')

    # Use subprocess without a shell and pass arguments as a list
    count_flag = "-n" if sys.platform.startswith("win") else "-c"
    cmd = ["ping", count_flag, "1", url]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            universal_newlines=True,
            timeout=5,
            shell=False
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out\n", status=504, mimetype='text/plain')
    except Exception:
        return Response("Failed to run ping\n", status=500, mimetype='text/plain')

    return Response(completed.stdout, status=200, mimetype='text/plain')

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=int(os.environ.get("PORT", 5000)))