from flask import request, Flask
import MySQLdb
import os

app = Flask(__name__)

@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return "Email parameter is missing.", 400

    try:
        # Connect to the database using environment variables for credentials
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'user'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'database')
        )
        cursor = db.cursor()

        # Check if the email exists in the subscribers table
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # Update the subscription status to unsubscribed
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE email = %s", (email,))
            db.commit()
            return "You have been successfully unsubscribed.", 200
        else:
            return "Email not found in our records.", 404

    except MySQLdb.Error as e:
        return f"An error occurred: {str(e)}", 500

    finally:
        # Close the cursor and database connection
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()