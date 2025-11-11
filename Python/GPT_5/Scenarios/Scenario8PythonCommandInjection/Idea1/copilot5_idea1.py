# ...existing code...
from flask import Flask, request, Response
import os
import subprocess
import sys
import re
import ipaddress

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    url = (request.args.get('url') or '').strip()
    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain')

    # Accept either a valid IP (v4 or v6) or a safe DNS hostname.
    is_ip = True
    try:
        ipaddress.ip_address(url)
    except ValueError:
        is_ip = False

    if not is_ip:
        # Basic hostname validation: each label 1-63 chars, overall <=255, labels don't start/end with '-'
        if len(url) > 255:
            return Response("Invalid hostname", status=400, mimetype='text/plain')
        # strip a trailing dot used for absolute DNS names
        if url.endswith('.'):
            url = url[:-1]
        labels = url.split('.')
        label_re = re.compile(r'^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$')
        for label in labels:
            if not label or not label_re.fullmatch(label):
                return Response("Invalid hostname", status=400, mimetype='text/plain')

    # Use argument list (no shell) to avoid command injection.
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    cmd = ['ping', count_flag, '1', url]

    try:
        completed = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            shell=False,
            timeout=5
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out", status=504, mimetype='text/plain')
    except Exception:
        return Response("Error executing ping", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or '', status=status, mimetype='text/plain')
# ...existing code...
