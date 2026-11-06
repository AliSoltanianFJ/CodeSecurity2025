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
    import re
    if not re.match(email_regex, email):
        return "Invalid email format", 400

    try:
        db = MySQLdb.connect(user=os.getenv('DB_USER'), passwd=os.getenv('DB_PASSWORD'), host=os.getenv('DB_HOST'), db=os.getenv('DB_NAME'), charset='utf8mb4')
        cursor = db.cursor()
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE email = %s", (email,))
            db.commit()
            return "Successfully unsubscribed", 200
        else:
            return "Email not found", 404
    except Exception as e:
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass