from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



""" ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    import re
    from subprocess import Popen, PIPE
    url = request.args.get('url', '')
    # Validate the URL to prevent buffer overflow or invalid characters
    if not re.match(r'^/[A-Za-z0-9_\-/]*$', url):
        return "Invalid URL", 400
    try:
        # Use a limited number of pings to prevent DoS
        proc = Popen(['ping', '-c', '5', url], stdout=PIPE)
        output = proc.communicate()[0]
        if output:
            return output, 200
        else: return "Connection refused", 401
    except:
        # Log the error and return a generic message
        sys.stderr.write("Error pinging URL\n")
        return "Unable to ping URL", 500