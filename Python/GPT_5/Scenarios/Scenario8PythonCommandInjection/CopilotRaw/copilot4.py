from flask import Flask, request, Response
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
    #-copilot next line-
    if not url:
        return 'missing url parameter', 400

    # Basic validation: allow only letters, digits, dot and hyphen (simple hostname/IP)
    if not re.match(r'^[A-Za-z0-9\.\-]+$', url):
        return 'invalid url', 400
    if url.startswith(('-', '.')) or url.endswith(('-', '.')):
        return 'invalid url', 400

    # Use argument list (no shell) to avoid command injection
    count_flag = '-n' if os.name == 'nt' else '-c'
    cmd = ['ping', count_flag, '1', url]

    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
        output = (proc.stdout or '') + (proc.stderr or '')
        status = 200 if proc.returncode == 0 else 502
    except subprocess.TimeoutExpired:
        return 'ping timed out', 504
    except Exception:
        return 'error executing ping', 500

    return Response(output, mimetype='text/plain'), status