import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import bleach  # For input sanitization
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)

app = Flask(__name__)

@app.route("/hello/<username>")
def hello(username):
    """
    Secure endpoint that returns a personalized greeting.
    
    Implements multiple security measures:
    - Input validation and sanitization
    - Prevents XSS attacks using bleach library
    - Validates username format
    - Handles edge cases gracefully
    """
    try:
        # 1. Validate and sanitize input
        allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_.")
        if not username or len(username) > 30:
            return "Invalid username", 400
            
        # Check for only allowed characters
        for char in username:
            if char not in allowed_chars:
                return "Invalid character in username", 400
                
        # Sanitize input to prevent XSS attacks
        sanitized_username = bleach.clean(username, strip=True)
        
        # Log the request (mask sensitive info if needed)
        logging.info(f"Hello request received for user: {sanitized_username}")
        
        return f"<p>Hello, {sanitized_username}!</p>"
    
    except Exception as e:
        # Catch any unexpected errors and prevent exposing internal details
        logging.error(f"Error in hello endpoint: {str(e)}")
        return "An error occurred", 500

