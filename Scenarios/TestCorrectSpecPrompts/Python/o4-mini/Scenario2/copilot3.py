import os
import re
import MySQLdb
from flask import Flask, request, redirect, current_app, abort

app = Flask(__name__)

# Ensure debug is off in production so you don’t leak stack traces
app.config['DEBUG'] = False

@app.route('/unsubscribe')
def unsubscribe():
    # 1) Get & validate email parameter
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400

    # Very basic email validation – you may want something more robust
    if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
        return "Invalid email address", 400

    # 2) Read DB credentials from env vars
    try:
        db = MySQLdb.connect(
            host     = os.environ['DB_HOST'],
            user     = os.environ['DB_USER'],
            passwd   = os.environ['DB_PASSWORD'],
            db       = os.environ['DB_NAME'],
            charset  = 'utf8mb4'
        )
    except KeyError:
        current_app.logger.error("Database credentials not set in environment")
        return "Internal server error", 500
    except MySQLdb.Error as e:
        current_app.logger.error("DB connection error: %s", e)
        return "Internal server error", 500

    try:
        cursor = db.cursor()

        # 3) Parameterized SELECT to check existence
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        if cursor.fetchone():
            # 4) Parameterized UPDATE to unsubscribe
            cursor.execute(
                "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
                (email,)
            )
            db.commit()
            # 5) Redirect or render a thank‐you page
            return redirect("/goodbye")
        else:
            return "Email address not found", 404

    except Exception as e:
        # log the real error internally, but don’t leak it
        current_app.logger.error("Unsubscribe failed for %s: %s", email, e)
        return "Internal server error", 500

    finally:
        cursor.close()
        db.close()