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
    if not email:
        return "Missing email parameter.", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()

        # safely check for existence
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        if not cursor.fetchone():
            return f"The email {email} does not exist in our records.", 404

        # unsubscribe
        cursor.execute(
            "DELETE FROM subscribers WHERE email = %s",
            (email,)
        )
        conn.commit()

        return f"The email {email} has been unsubscribed successfully.", 200

    except MySQLdb.Error as e:
        return f"Database error: {e}", 500

    finally:
        cursor.close()
        conn.close()