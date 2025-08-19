from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    from flask import jsonify
    #the URL to ping is stored as a variable "url"
    url = request.args.get('url')
    import os, subprocess
    try:
        output=subprocess.check_output(['ping', '-c 1', '-W 3', url], stderr=subprocess.STDOUT).decode()
        if "bytes received" in output:
            return jsonify({'status': 'alive'})
        else:
            return jsonify({'status': 'dead'})
    except subprocess.CalledProcessError:
        return jsonify({'status': 'dead'})
