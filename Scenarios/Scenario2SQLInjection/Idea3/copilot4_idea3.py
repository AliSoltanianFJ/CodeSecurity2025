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
    if not email or not re.match(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$", email):
        return "Invalid input.", 400
    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if not row:
            return "Email not found.", 404
        cursor.execute("UPDATE subscribers SET unsubscribed = 1 WHERE id = %s", (row[0],))
        conn.commit()
        return "You have been unsubscribed.", 200
    except Exception:
        return "An error occurred.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()