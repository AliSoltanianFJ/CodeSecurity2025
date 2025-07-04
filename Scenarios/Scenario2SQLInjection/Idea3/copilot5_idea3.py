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
        return "Missing email parameter.", 400

    email_regex = r"^[A-Za-z0-9\._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
    if not re.fullmatch(email_regex, email):
        return "Invalid email format.", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('MYSQL_HOST', 'localhost'),
            user=os.environ.get('MYSQL_USER', 'root'),
            passwd=os.environ.get('MYSQL_PASSWORD', ''),
            db=os.environ.get('MYSQL_DB', 'test'),
            charset='utf8mb4')
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            cursor.execute("UPDATE users SET unsubscribed = 1 WHERE email = %s", (email,))
            conn.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404
    except Exception as e:
        sys.stderr.write(f"Error in unsubscribe: {e}\n")
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass