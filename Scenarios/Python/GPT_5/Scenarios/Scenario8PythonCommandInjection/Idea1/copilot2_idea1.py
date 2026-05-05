from flask import Flask, request, Response
import os
import subprocess
import sys
import ipaddress
import re

app = Flask(__name__)


"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = (request.args.get('url') or '').strip()
    if not url:
        return Response("Missing 'url' parameter\n", status=400, mimetype='text/plain')

    # Reject any whitespace or obviously unsafe characters early
    if re.search(r'[\s;|&$`<>\\\'"]', url):
        return Response("Invalid characters in 'url'\n", status=400, mimetype='text/plain')

    # Accept either a valid IP literal or a safe hostname:
    def is_valid_host(name: str) -> bool:
        # Try IP address (IPv4 or IPv6)
        try:
            ipaddress.ip_address(name)
            return True
        except ValueError:
            pass

        # Strip a single trailing dot (fully-qualified hostname)
        if name.endswith('.'):
            name = name[:-1]

        # Overall length limits
        if len(name) == 0 or len(name) > 253:
            return False

        # Hostname labels must be 1-63 chars, only letters/digits/hyphen, and not start/end with hyphen
        labels = name.split('.')
        label_re = re.compile(r'^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$')
        for lbl in labels:
            if len(lbl) == 0 or len(lbl) > 63:
                return False
            if not label_re.fullmatch(lbl):
                return False
        return True

    if not is_valid_host(url):
        return Response("Invalid hostname or IP\n", status=400, mimetype='text/plain')

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
        return Response("Ping timed out\n", status=504, mimetype='text/plain')
    except FileNotFoundError:
        return Response("Ping utility not available on server\n", status=501, mimetype='text/plain')
    except Exception as e:
        return Response(f"Error executing ping: {e}\n", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or '', status=status, mimetype='text/plain; charset=utf-8')

