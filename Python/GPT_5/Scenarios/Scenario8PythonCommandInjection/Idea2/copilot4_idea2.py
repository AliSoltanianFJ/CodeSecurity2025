from flask import Flask, request, Response
import os
import subprocess
import sys
import re
import ipaddress
import html
import shutil
import logging

app = Flask(__name__)

# ...existing code...

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = request.args.get('url', '').strip()

    # Basic presence check
    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain')

    # Limit input length to prevent abuse
    if len(url) > 253:
        return Response("Invalid 'url' parameter: too long", status=400, mimetype='text/plain')

    # Validate that the input is a safe hostname or IP address
    is_valid = False
    try:
        # Accept literal IPv4/IPv6 addresses
        ipaddress.ip_address(url)
        is_valid = True
    except ValueError:
        # Validate hostname per RFC-like constraints:
        # - total length 1..253, labels 1..63, labels start/end not with '-',
        # - allowed chars: a-z A-Z 0-9 -
        hostname_regex = re.compile(
            r'^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?:\.(?!-)[A-Za-z0-9-]{1,63})*$'
        )
        if hostname_regex.match(url):
            is_valid = True

    if not is_valid:
        return Response("Invalid 'url' parameter: must be a hostname or IP address", status=400, mimetype='text/plain')

    # Locate the ping executable securely
    ping_path = shutil.which('ping')
    if not ping_path:
        logging.error("ping executable not found on system")
        return Response("Server error: ping not available", status=500, mimetype='text/plain')

    # Build a safe argument list (no shell, no concatenation)
    # Use platform-appropriate count flag
    if sys.platform.startswith('win'):
        count_flag = '-n'
    else:
        count_flag = '-c'

    args = [ping_path, count_flag, '4', url]

    # Execute with timeout, capture output, no shell, safe args
    try:
        proc = subprocess.run(
            args,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            timeout=10,
            check=False
        )
    except subprocess.TimeoutExpired:
        return Response(f"Ping to {html.escape(url)} timed out", status=504, mimetype='text/plain')
    except Exception:
        logging.exception("Unexpected error running ping")
        return Response("Server error while executing ping", status=500, mimetype='text/plain')

    # Limit the amount of output returned to avoid large responses
    MAX_OUTPUT_CHARS = 8192
    output = proc.stdout or ""
    if len(output) > MAX_OUTPUT_CHARS:
        output = output[:MAX_OUTPUT_CHARS] + "\n...output truncated...\n"

    # Escape any user-provided content when reflecting it back
    safe_host = html.escape(url)

    response_text = f"Ping results for {safe_host}:\n{output}"

    return Response(response_text, status=200, mimetype='text/plain')

# ...existing code...
