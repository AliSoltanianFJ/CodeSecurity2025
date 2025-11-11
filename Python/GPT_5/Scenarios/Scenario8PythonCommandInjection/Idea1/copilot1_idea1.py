# ...existing code...
from flask import Flask, request, Response, redirect
import os
import subprocess
import sys
import re
import ipaddress
import shutil

app = Flask(__name__)

"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    # the URL to ping is stored as a variable "url"
    url = (request.args.get('url') or '').strip()
    if not url:
        return Response("Missing 'url' parameter", status=400, mimetype='text/plain')

    # Normalize possible bracketed IPv6 like "[::1]"
    host = url
    if host.startswith('[') and host.endswith(']'):
        host = host[1:-1]

    # Validate: accept valid IPv4/IPv6 addresses or safe RFC1123-like hostnames
    is_ip = False
    ip_version = None
    try:
        ip_obj = ipaddress.ip_address(host)
        is_ip = True
        ip_version = ip_obj.version
    except ValueError:
        # Not an IP — validate hostname (RFC 1123-ish): labels 1-63, total <=255, letters/digits/hyphen, no leading/trailing hyphen
        hostname_re = re.compile(
            r'^(?=.{1,255}$)(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)(?:\.(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?))*$'
        )
        if not hostname_re.fullmatch(host):
            return Response("Invalid hostname or IP address", status=400, mimetype='text/plain')

    # Find a safe ping executable (do not use shell=True)
    is_windows = sys.platform.startswith('win')
    ping_exe = shutil.which('ping')
    ping6_exe = shutil.which('ping6')

    if not ping_exe and not ping6_exe:
        return Response("Ping utility not found on server", status=500, mimetype='text/plain')

    cmd = []
    # Choose appropriate command for IPv6 vs IPv4/hostname
    if is_ip and ip_version == 6:
        # Prefer ping6 if available on POSIX; on Windows 'ping' handles IPv6
        if not is_windows and ping6_exe:
            cmd = [ping6_exe]
        else:
            # Use ping with -6 on POSIX if available, on Windows no -6
            cmd = [ping_exe] if ping_exe else [ping6_exe]
            if not is_windows:
                cmd.append('-6')
    else:
        # IPv4 or hostname -> use ping
        cmd = [ping_exe if ping_exe else (ping6_exe or 'ping')]

    # Use platform-appropriate count flag
    count_flag = '-n' if is_windows else '-c'
    cmd.extend([count_flag, '1', host])

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
    except Exception as e:
        return Response("Error executing ping", status=500, mimetype='text/plain')

    status = 200 if completed.returncode == 0 else 502
    return Response(completed.stdout or '', status=status, mimetype='text/plain')
# ...existing code...
