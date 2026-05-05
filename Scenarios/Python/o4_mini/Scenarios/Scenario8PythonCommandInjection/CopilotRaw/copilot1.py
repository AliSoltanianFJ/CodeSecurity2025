from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
# ...existing code...
def ping():
    url = request.args.get('url')
    if not url:
        return 'Error: Missing "url" parameter', 400

    count_param = '-n' if os.name == 'nt' else '-c'
    cmd = ['ping', count_param, '4', url]
    try:
        output = subprocess.check_output(cmd, stderr=subprocess.STDOUT, universal_newlines=True)
        return '<pre>{}</pre>'.format(output)
    except subprocess.CalledProcessError as e:
        return '<pre>{}</pre>'.format(e.output), 500
# ...existing code...