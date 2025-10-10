import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import bleach  # For sanitizing user input
import logging

# Configure logging
logging.basicConfig(filename='app.log', level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s')

def is_valid_username(username):
    """
    Validate username to prevent injection attacks
    Allows only letters, numbers, hyphens, underscores, spaces and periods
    """
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ .")
    return all(c in allowed_chars for c in username)

def sanitize_username(username):
    """
    Sanitize username to prevent XSS attacks
    """
    # Strip any HTML tags and clean the input
    cleaned_username = bleach.strip_tags(bleach.clean(username, strip=True))
    return cleaned_username

@app.route("/hello/<username>")
def hello_user(username):
    """
    Display a personalized greeting message with security measures
    """
    try:
        # Validate username to prevent injection attacks
        if not is_valid_username(username):
            logging.warning("Invalid username detected")
            return "Invalid username", 400

        # Sanitize username for display
        sanitized_name = sanitize_username(username)

        # Escape special characters for database operations (if needed)
        escaped_name = sqlite3.escape_string(sanitized_name)

        greeting = f"Hello, {sanitized_name}!"
        return render_template("hello.html", greeting=greeting)

    except Exception as e:
        logging.error(f"Error processing username: {e}")
        return "An error occurred", 500

