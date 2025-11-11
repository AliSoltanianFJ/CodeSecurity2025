# ...existing code...
from flask import Flask, request, Response
import os
import subprocess
import sys
import shutil
import ipaddress
import urllib.parse
import re
import html
import logging

app = Flask(__name__)

# Simple logger (avoid exposing sensitive info)
logger = logging.getLogger(__name__)
logger.addHandler(logging.NullHandler())

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url', '')
    # Basic input length check
    if not url or len(url) > 2083:
        return Response("Invalid or missing 'url' parameter.", status=400, mimetype='text/plain')

    # Extract hostname from URL or treat input as hostname
    parsed = urllib.parse.urlparse(url if '://' in url else '//' + url, scheme='')
    host = parsed.hostname or url
    host = host.strip()

    # Reject obviously malicious or empty host
    if not host or len(host) > 255:
        return Response("Invalid host.", status=400, mimetype='text/plain')

    # Validate host: either a safe IP address or a valid DNS name
    def is_disallowed_ip(ip_str):
        try:
            ip = ipaddress.ip_address(ip_str)
        except ValueError:
            return False  # not an IP
        # reject private/reserved/loopback/multicast/link-local addresses
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast:
            return True
        return False

    # If host is an IP, validate it's public
    try:
        if re.match(r'^\[?[0-9a-fA-F:.]+\]?$', host):  # crude detection for IPv4/IPv6
            # strip brackets for IPv6 like [::1]
            cleaned_ip = host.strip('[]')
            if is_disallowed_ip(cleaned_ip):
                return Response("Refusing to ping private or reserved addresses.", status=403, mimetype='text/plain')
            # normalize ip to ensure it's a valid IP
            ipaddress.ip_address(cleaned_ip)
            safe_host = cleaned_ip
        else:
            # Validate DNS name according to RFC-label rules (no leading/trailing hyphens, label length limits)
            # Allow internationalized names by encoding to IDNA
            try:
                idna_host = host.encode('idna').decode('ascii')
            except Exception:
                return Response("Invalid hostname.", status=400, mimetype='text/plain')

            # Validate labels
            label_re = re.compile(r'^(?!-)[A-Za-z0-9-]{1,63}(?<!-)$')
            labels = idna_host.split('.')
            if any(not label_re.match(lbl) for lbl in labels):
                return Response("Invalid hostname.", status=400, mimetype='text/plain')

            safe_host = idna_host
    except Exception:
        return Response("Invalid host.", status=400, mimetype='text/plain')

    # Ensure ping binary is available and use safe argument list (no shell)
    ping_cmd = shutil.which('ping')
    if not ping_cmd:
        logger.error("ping binary not found")
        return Response("Server configuration error.", status=500, mimetype='text/plain')

    # Choose count flag based on platform
    count_flag = '-n' if os.name == 'nt' else '-c'

    # Build argument list without invoking a shell to prevent injection
    args = [ping_cmd, count_flag, '4', safe_host]

    try:
        # Run with timeout and capture output safely
        proc = subprocess.run(
            args,
            capture_output=True,
            text=True,
            timeout=8,
            check=False
        )
    except subprocess.TimeoutExpired:
        return Response("Ping timed out.", status=504, mimetype='text/plain')
    except Exception:
        logger.exception("Error executing ping")
        return Response("Server error while executing ping.", status=500, mimetype='text/plain')

    # Prepare output: prefer stdout, include limited stderr if present; truncate to avoid huge responses
    output = proc.stdout or proc.stderr or ''
    MAX_OUT = 4000
    if len(output) > MAX_OUT:
        output = output[:MAX_OUT] + "\n...[truncated]"

    # Escape output to avoid any accidental HTML/script injection if served in a browser
    safe_output = html.escape(output)

    return Response(safe_output, status=200, mimetype='text/plain')
# ...existing code...
