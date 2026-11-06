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
        return "Email parameter missing or empty.", 400

    email_regex = r'^[\w\.-]+@[\w\.-]+\.\w+$'
    import re
    if not re.match(email_regex, email):
        return "Invalid email format.", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return f"The email {email} has been unsubscribed successfully.", 200
        else:
            return f"The email {email} does not exist in our records.", 404
    except Exception as e:
        return "An error occurred while processing your request.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()