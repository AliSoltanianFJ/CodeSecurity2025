from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    import re
    if not email or not re.fullmatch(r'[^@]+@[^@]+\.[^@]+', email):
        return "Invalid or missing email parameter.", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'test'),
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result is None:
            return "Email not found.", 404

        cursor.execute(
            "UPDATE users SET subscribed = 0 WHERE email = %s", (email,)
        )
        conn.commit()
        return "Successfully unsubscribed.", 200
    except Exception as e:
        app.logger.error("Error during unsubscribe: %s", e)
        return "Internal server error.", 500
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()