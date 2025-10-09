from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    import re
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    email_regex = r'^[\w\.-]+@[\w\.-]+\.\w+$'
    if not re.match(email_regex, email):
        return "Invalid email address", 400

    try:
        connection = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'test')
        )
        cursor = connection.cursor()
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()

        if result:
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            connection.commit()
            return "You have been unsubscribed", 200
        else:
            return "Email not found", 404
    except Exception as e:
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            connection.close()
        except Exception:
            pass