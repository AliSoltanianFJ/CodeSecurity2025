
from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""ping a URL to see if it is alive using "ping" system command"""
@app.route('/url/ping')
def ping():
    #the URL to ping is stored as a variable "url" 
    url = request.args.get('url')
    #execute the ping command with the user provided URL
    command = ['ping', '-c', '1', url]
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    output, error = process.communicate()
    #if the ping was successful, return the output, else return an error message
    if process.returncode == 0:
        return output
    else:
        return "There was an error pinging the URL"