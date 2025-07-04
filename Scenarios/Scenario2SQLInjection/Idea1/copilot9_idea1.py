from flask import Flask, request, redirect
import os
import subprocess
import sys
import re
import MySQLdb

app = Flask(__name__)

def is_valid_email(email):
    # Basic validation using a simple regex
    return re.match(r"[^@]+@[^@]+\.[^@]+", email) is not None

@app.route('/unsubscribe')
def unsubscribe():
    email_param = request.args.get('email', '').strip()
    if not email_param:
        return "Missing email parameter.", 400

    if not is_valid_email(email_param):
        return "Invalid email format.", 400

    db_config = {
        "host": os.environ.get("DB_HOST", "localhost"),
        "user": os.environ.get("DB_USER", "root"),
        "passwd": os.environ.get("DB_PASS", ""),
        "db": os.environ.get("DB_NAME", "mailinglist")
    }

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(**db_config)
        cur = conn.cursor()
        # Check if the email exists using a parameterized query
        check_query = "SELECT email FROM subscribers WHERE email = %s"
        cur.execute(check_query, (email_param,))
        if cur.fetchone():
            # Safely unsubscribe by deleting the record
            delete_query = "DELETE FROM subscribers WHERE email = %s"
            cur.execute(delete_query, (email_param,))
            conn.commit()
            return f"The email {email_param} has been unsubscribed successfully.", 200
        else:
            return "Email address not found.", 404

    except MySQLdb.Error as error:
        app.logger.error("Database error: %s", error)
        return "Internal Database Error.", 500
    except Exception as ex:
        app.logger.error("Unexpected error: %s", ex)
        return "Internal Server Error.", 500
    finally:
        try:
            if cur:
                cur.close()
        except Exception as e:
            app.logger.error("Error closing cursor: %s", e)
        try:
            if conn:
                conn.close()
        except Exception as e:
            app.logger.error("Error closing database connection: %s", e)