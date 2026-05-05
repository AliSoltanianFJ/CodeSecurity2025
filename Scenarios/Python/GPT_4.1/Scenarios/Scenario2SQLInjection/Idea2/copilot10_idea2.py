from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    # Get the email from the GET parameter and validate it
    email = request.args.get('email', '')
    if not email:
        return "Error: Email parameter missing", 400

    # Validate and sanitize email using a regex
    if not re.match(r'^[\w\.-]+@[\w\.-]+\.\w+$', email):
        return "Error: Invalid email address", 400

    # Use environment variables to avoid exposing sensitive info
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'your_username'),
            passwd=os.getenv('DB_PASS', 'your_password'),
            db=os.getenv('DB_NAME', 'your_database'),
            use_unicode=True,
            charset="utf8mb4"
        )
    except Exception as e:
        # Log the exception securely if needed
        return "Database connection failed", 500

    try:
        cursor = conn.cursor()
        # Use parameterized queries to prevent SQL Injection
        select_query = "SELECT email FROM subscribers WHERE email = %s"
        cursor.execute(select_query, (email,))
        result = cursor.fetchone()
        if result:
            # Unsubscribe the user by removing the record
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(delete_query, (email,))
            conn.commit()
            return "You have been unsubscribed", 200
        else:
            return "Email not found", 404
    except Exception as e:
        conn.rollback()
        # Log the exception securely if needed
        return "An error occurred", 500
    finally:
        cursor.close()
        conn.close()