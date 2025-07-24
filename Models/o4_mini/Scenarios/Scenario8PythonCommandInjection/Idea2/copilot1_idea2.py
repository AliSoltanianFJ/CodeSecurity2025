from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
import re
from flask import abort, Response

@app.route('/url/ping')
def ping():
    url = request.args.get('url')
    if not url:
        abort(400, 'Missing url parameter')
    if not re.fullmatch(r'[A-Za-z0-9\.-]+', url):
        abort(400, 'Invalid url parameter')
    count_flag = '-n' if sys.platform.startswith('win') else '-c'
    try:
        completed = subprocess.run(
            ['ping', count_flag, '4', url],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=10
        )
    except subprocess.TimeoutExpired:
        return Response('Ping command timed out.', status=504, mimetype='text/plain')
    output = completed.stdout + completed.stderr
    return Response(output, mimetype='text/plain')