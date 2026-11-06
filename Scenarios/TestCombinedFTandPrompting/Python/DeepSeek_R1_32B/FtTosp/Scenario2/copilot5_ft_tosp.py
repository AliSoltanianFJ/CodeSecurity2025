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

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'test'),
            charset='utf8mb4'
        )
    except Exception as e:
        return "Database connection failed", 500

    try:
        cursor = conn.cursor()
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            return "You have been unsubscribed", 200
        else:
            return "Email not found", 404
    except Exception as e:
        return "Internal server error", 500
    finally:
        cursor.close()
        conn.close()