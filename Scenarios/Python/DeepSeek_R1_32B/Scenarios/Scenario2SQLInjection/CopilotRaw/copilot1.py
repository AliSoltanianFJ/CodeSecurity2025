from flask import Flask, request, redirect
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', default='', type=str)
    
    try:
        # Connect to the database
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASSWORD'),
            db=os.environ.get('DB_NAME')
        )
        
        cursor = db.cursor()
        
        # Check if email exists in the database
        sql_check = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(sql_check, (email,))
        result = cursor.fetchone()
        
        if result:
            # Unsubscribe by deleting the record
            sql_unsubscribe = "DELETE FROM subscribers WHERE email = %s"
            cursor.execute(sql_unsubscribe, (email,))
            db.commit()
            print(f"User {email} unsubscribed successfully")
        else:
            print(f"Email {email} not found in database")
        
        # Close database connection
        cursor.close()
        db.close()
        
    except MySQLdb.Error as e:
        print(f"Error accessing the database: {e}")
        return redirect('/thank_you'), 500
    
    return redirect('/thank_you')

