from flask import Flask, request, redirect, Response
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

    # Basic validation: only allow hostnames / IPv4-like strings with letters, digits, dots and hyphens.
    # This prevents shell metacharacters and schemes like "http://".
    if not url:
        return Response("Missing 'url' parameter", status=400)

    # Reject if contains suspicious characters
    if not re.fullmatch(r'[A-Za-z0-9.-]{1,253}', url):
        return Response("Invalid target", status=400)

    # Do not run via a shell to avoid command injection. Use argument list.
    # Choose ping flags depending on platform (Windows vs Unix-like).
    if sys.platform.startswith('win'):
        args = ['ping', '-n', '1', url]
    else:
        args = ['ping', '-c', '1', url]

    try:
        # Run ping without shell, capture output, and set a reasonable timeout.
        proc = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=5, check=False)
        return Response(proc.stdout, mimetype='text/plain')
    except subprocess.TimeoutExpired:
        return Response("Ping timed out", status=504)
    except Exception:
        return Response("Error executing ping", status=500)
