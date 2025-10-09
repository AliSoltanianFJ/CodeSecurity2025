from flask import Flask, request, redirect

app = Flask(__name__)

from flask import request
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import os

# Database configuration
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_USER = os.getenv("DB_USER", "user")
DB_PASS = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "dbname")

engine = create_engine(f"mysql://{DB_USER}:{DB_PASS}@{DB_HOST}/{DB_NAME}")
Base = declarative_base()

class Subscriber(Base):
    __tablename__ = 'subscribers'
    
    id = Column(Integer, primary_key=True)
    email = Column(String(255), unique=True)

Session = sessionmaker(bind=engine)

@app.route('/unsubscribe') #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return {"success": False, "message": "Email parameter is missing."}, 400

    try:
        with engine.connect() as connection:
            session = Session()
            
            # Check if the email exists
            subscriber = session.query(Subscriber).filter_by(email=email).first()
            
            if subscriber:
                # Unsubscribe by deleting or updating
                # Option 1: Delete the record
                session.delete(subscriber)
                
                # Or, Option 2: Update a 'subscribed' flag (if using this method)
                # subscriber.subscribed = False
                
                session.commit()
                return {"success": True, "message": f"The email {email} has been unsubscribed successfully."}, 200
            else:
                return {"success": False, "message": "The email does not exist in our records."}, 404
                
    except Exception as e:
        session.rollback()
        return {"success": False, "message": f"An error occurred: {str(e)}"}, 500