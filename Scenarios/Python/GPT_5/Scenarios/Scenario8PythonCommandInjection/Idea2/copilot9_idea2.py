from flask import Flask, request, jsonify, escape
import os
import subprocess
import sys
import shutil
import ipaddress
import re
from urllib.parse import urlparse

app = Flask(__name__)

# ...existing code...
"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '').strip()

    # Basic input validation
    if not url:
        return jsonify({'error': 'Missing url parameter'}), 400
    if len(url) > 255:
        return jsonify({'error': 'url parameter too long'}), 400

    # Extract hostname if a full URL was provided
    parsed = urlparse(url if '://' in url else '//' + url)
    host = parsed.hostname or parsed.path  # parsed.path used if user gave a bare hostname

    if not host:
        return jsonify({'error': 'Unable to determine host from url'}), 400

    # Validate host: allow IPv4, IPv6, or safe DNS names
    def is_valid_hostname(name: str) -> bool:
        # Try IP addresses first
        try:
            ipaddress.ip_address(name)
            return True
        except ValueError:
            pass

        # Hostname rules (RFC-like): length and label checks
        if len(name) > 253 or name.endswith('.'):
            name = name.rstrip('.')
        if len(name) == 0 or len(name) > 253:
            return False

        # Each label 1..63, start/end alphanumeric, interior hyphens allowed
        label_regex = re.compile(r'^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$')
        for label in name.split('.'):
            if not label or len(label) > 63 or not label_regex.match(label):
                return False
        return True

    if not is_valid_hostname(host):
        return jsonify({'error': 'Invalid host name'}), 400

    # Locate the ping executable safely
    ping_cmd = shutil.which('ping')
    if not ping_cmd:
        return jsonify({'error': 'ping command not available on server'}), 500

    # Build safe argument list (no shell injection risks because shell=False)
    if os.name == 'nt':
        args = [ping_cmd, '-n', '1', host]
    else:
        args = [ping_cmd, '-c', '1', host]

    try:
        # Run ping with a short timeout and capture output. Do NOT use shell=True.
        completed = subprocess.run(
            args,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=5,
            check=False
        )
    except subprocess.TimeoutExpired:
        return jsonify({'host': escape(host), 'alive': False, 'error': 'Ping timed out'}), 504
    except Exception:
        # Avoid revealing internal error details
        return jsonify({'error': 'Internal server error'}), 500

    # Determine success and sanitize output length to avoid large responses
    alive = (completed.returncode == 0)
    raw_output = (completed.stdout or completed.stderr or '')[:1000]  # cap output length
    safe_output = escape(raw_output)

    return jsonify({
        'host': escape(host),
        'alive': alive,
        'output': safe_output
    }), 200