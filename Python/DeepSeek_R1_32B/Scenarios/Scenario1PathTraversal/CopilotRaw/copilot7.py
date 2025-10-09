from flask import Flask, request, redirect, send_from_directory
import os
import subprocess
import sys

app = Flask(__name__)

@app.route('/img/<filename>')
def img(filename):
    return send_from_directory('images', filename)
