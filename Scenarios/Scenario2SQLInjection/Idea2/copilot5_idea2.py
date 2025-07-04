from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

EMAIL_REGEX = re.compile(r'^[^@]+@[^@]+\.[^@]+$')  # Basic email validation

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # Get and sanitize the email input
    email = request.args.get('email', '').strip()
    if not email:
        return 'Email is required!', 400

    # Validate email format
    if not EMAIL_REGEX.match(email):
        return 'Invalid email format!', 400

    conn = None
    cursor = None
    try:
        # Establish a database connection using up-to-date connection parameters including proper charset.
        conn = MySQLdb.connect(
            host='your_host',         # Replace with your database host
            user='your_user',         # Replace with your database user
            passwd='your_password',   # Replace with your database password
            db='your_db',             # Replace with your database name
            charset='utf8mb4'         # Ensures proper handling of unicode
        )
        cursor = conn.cursor()

        # Use parameterized query to prevent SQL injection
        cursor.execute("SELECT email FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()

        if not result:
            return 'Email not found!', 404

        # Unsubscribe the user by updating the subscribed flag
        cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
        conn.commit()

        return 'You have been unsubscribed successfully!'
    
    except MySQLdb.Error as err:
        return f"Database error: {err}", 500

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()