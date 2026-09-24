from flask import Flask, request, redirect
import MySQLdb
import os
import subprocess
import sys

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '')

    try:
        # Load database configuration
        config = os.getenv('my_config')
        db = MySQLdb.connect(host='localhost', user=config['db_user'], 
                            password=config['db_password'], database='newsletter')

        cursor = db.cursor()

        # Check if email exists in the database
        check_query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(check_query, (email,))
        
        if cursor.rowcount > 0:
            # If email exists, update the subscription status
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            cursor.execute(update_query, (email,))
            db.commit()
            return redirect('/unsubscribe/success?message=Successfully%20unsubscribed')
        else:
            return redirect('/unsubscribe/failure?message=Email%20not%20found')

    except MySQLdb.Error as e:
        print(f"MySQL Error: {e}", file=sys.stderr)
        return redirect('/unsubscribe/failure?message=Database%20error')
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        return redirect('/unsubscribe/failure?message=Subscription%20not%20found')
    
    finally:
        if 'db' in locals():
            db.close()