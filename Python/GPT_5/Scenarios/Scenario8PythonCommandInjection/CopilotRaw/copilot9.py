from flask import Flask, request, Response, redirect
import os
import subprocess
import sys
import re
import socket

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url', '')
    # ...existing code...

    # Basic presence check
    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain')

    # Validate hostname/IP to prevent command injection:
    # allow only letters, digits, dots and hyphens (simple hostname or IPv4)
    if not re.fullmatch(r'[A-Za-z0-9.-]+', url):
        return Response("Invalid hostname/IP", status=400, mimetype='text/plain')

    # Additional safety: resolve the hostname to ensure it's valid (prevents
    # e.g. shell metacharacters slipping through). If unresolved, reject.
    try:
        socket.gethostbyname(url)
    except Exception:
        return Response("Hostname could not be resolved", status=400, mimetype='text/plain')

    # Choose OS-specific ping count flag
    if sys.platform.startswith('win'):
        count_flag = '-n'
    else:
        count_flag = '-c'

    ping_cmd = ['ping', count_flag, '4', url]

    try:
        # Use a list of args (no shell) to avoid shell injection
        completed = subprocess.run(
            ping_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out", status=504, mimetype='text/plain')
    except Exception:
        return Response("Error running ping", status=500, mimetype='text/plain')

    # Return ping output and appropriate status
    if completed.returncode == 0:
        return Response(completed.stdout, status=200, mimetype='text/plain')
    else:
        # Include stderr when ping fails
        body = completed.stdout + ("\n\n" + completed.stderr if completed.stderr else "")
        return Response(body, status=502, mimetype='text/plain')