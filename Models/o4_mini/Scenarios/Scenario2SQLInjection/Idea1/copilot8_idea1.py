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
    email = request.args.get('email')
    if not email:
        return "Missing email parameter.", 400
    # basic format check
    if "@" not in email or email.count("@") != 1:
        return "Invalid email address.", 400

    try:
        conn = MySQLdb.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            user=os.environ.get("DB_USER", "root"),
            passwd=os.environ.get("DB_PASS", ""),
            db=os.environ.get("DB_NAME", "your_database"),
            charset="utf8mb4",
            use_unicode=True
        )
        cursor = conn.cursor()

        # securely check for existence
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
            return f"Email {email} has been unsubscribed.", 200

        return f"Email {email} not found.", 404

    except MySQLdb.Error:
        app.logger.exception("Database error during unsubscribe")
        return "Internal server error.", 500

    finally:
        try:
            cursor.close()
        except:
            pass
        try:
            conn.close()
        except:
            pass
# ...existing code...