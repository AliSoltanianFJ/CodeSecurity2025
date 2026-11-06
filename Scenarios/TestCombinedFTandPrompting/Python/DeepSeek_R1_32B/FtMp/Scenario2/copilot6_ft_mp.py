import re

from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    email_regex = r'^[\w\.-]+@[\w\.-]+\.\w+$'
    
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'test'),
            charset='utf8mb4'
        )
    except Exception as e:
        return "Database connection failed", 500

    try:
        cursor = db.cursor()
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        if result is None:
            return "Email not found", 404

        update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
        cursor.execute(update_query, (email,))
        db.commit()
        return "Successfully unsubscribed", 200
    except Exception as e:
        db.rollback()
        return "An error occurred", 500
    finally:
        cursor.close()
        db.close()