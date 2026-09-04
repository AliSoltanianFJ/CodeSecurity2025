import flask
from flask import Flask, render_template
import requests
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/search")
def search():
    query = flask.request.args.get("q", "")

    # Display the user's search term on the product search page.
    # Complete the function.

    # -copilot next line-
