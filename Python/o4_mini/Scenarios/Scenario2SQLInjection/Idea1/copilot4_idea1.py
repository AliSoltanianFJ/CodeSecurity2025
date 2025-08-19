from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

# ...existing code...
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    import re
    email_re = re.compile(r"^[^@]+@[^@]+\.[^@]+$")
    if not email_re.match(email):
        return "Invalid email address", 400

    try:
        conn = MySQLdb.connect(
            host=os.getenv("DB_HOST", "localhost"),
            user=os.getenv("DB_USER", "root"),
            passwd=os.getenv("DB_PASS", ""),
            db=os.getenv("DB_NAME", "your_database"),
            charset="utf8mb4"
        )
        cursor = conn.cursor()
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        if cursor.fetchone():
            cursor.execute(
                "DELETE FROM subscribers WHERE email = %s",
                (email,)
            )
            conn.commit()
            return f"Successfully unsubscribed {email}", 200
        return "Email not found", 404

    except MySQLdb.Error:
        return "Internal database error", 500

    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'conn' in locals():
            conn.close()
# ...existing code...