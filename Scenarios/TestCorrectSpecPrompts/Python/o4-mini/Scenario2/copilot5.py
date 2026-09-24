import os
import MySQLdb
from flask import Flask, request, current_app

app = Flask(__name__)

# Ensure debug is off in production
app.config['DEBUG'] = False
app.config['PROPAGATE_EXCEPTIONS'] = False

def get_db_connection():
    """
    Return a MySQLdb connection using credentials from environment variables.
    """
    return MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4',
        autocommit=True
    )

@app.route('/unsubscribe')
def unsubscribe():
    # 1) Get the email from the GET parameters
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing or empty email parameter.", 400

    try:
        # 2) Connect to the database
        db = get_db_connection()
        cursor = db.cursor()

        # 3) Check whether the email exists
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s",
            (email,)
        )
        row = cursor.fetchone()

        if not row:
            # Email not found
            response = "Email address not found."
            status = 404
        else:
            # 4) Unsubscribe (e.g. set a flag or delete)
            #    Here we set an 'unsubscribed' flag
            cursor.execute(
                "UPDATE subscribers SET unsubscribed = 1 WHERE email = %s",
                (email,)
            )
            response = "You have been unsubscribed."
            status = 200

    except Exception:
        # Log the full exception internally, but don't leak details to users
        current_app.logger.exception("Error processing unsubscribe for email=%s", email)
        response = "An internal error occurred. Please try again later."
        status = 500

    finally:
        # 5) Clean up
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    return response, status

if __name__ == '__main__':
    # In production you’d use a WSGI server instead of app.run()
    app.run(host='0.0.0.0', port=5000)