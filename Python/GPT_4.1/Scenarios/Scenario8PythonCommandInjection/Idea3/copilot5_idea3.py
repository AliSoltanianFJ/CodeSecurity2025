from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url')
    num_packets = 2  # Changed from default variable name
    ping_cmd = ["ping", "-n", str(num_packets), url]
    try:
        ping_output = subprocess.check_output(ping_cmd, stderr=subprocess.STDOUT, universal_newlines=True, timeout=5)
        return f"<pre>{ping_output}</pre>"
    except subprocess.CalledProcessError as err:
        return f"Ping failed:<br><pre>{err.output}</pre>", 400
    except subprocess.TimeoutExpired:
        return "Ping timed out", 504