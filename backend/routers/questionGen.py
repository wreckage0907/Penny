from fastapi import APIRouter, HTTPException
from fastapi.responses import JSONResponse
from schema.question import Question
import google.generativeai as genai
import os
from dotenv import load_dotenv
import logging
import json
import re

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
try:
    genai.configure(api_key=api_key)
    model = genai.GenerativeModel('gemini-1.5-flash')
    test_response = model.generate_content("Test")
    if not test_response.text:
        raise Exception("Invalid API key or model access")
except Exception as e:
    logger.error(f"Error configuring Gemini API: {str(e)}")
    raise

CHAPTERS = {
    "introduction_to_personal_finance": "Introduction to Personal Finance",
    "setting_financial_goals": "Setting Financial Goals",
    "budgeting_and_expense_tracking": "Budgeting and Expense Tracking",
    "online_and_mobile_banking": "Online and Mobile Banking"
}

router = APIRouter()

def generate_questions(chapter):
    prompt = f"""
    Generate 10 unique finance questions related to {chapter}. Each question should be suitable for someone learning about personal finance.
    
    For each question, provide:
    1. The question text
    2. Four answer options (A, B, C, D)
    3. The correct answer (just the letter)
    4. A brief hint
    
    Format the output as a JSON array of objects, where each object represents a question and follows this structure:
    {{
        "question": "Question text here",
        "options": ["A) Option A", "B) Option B", "C) Option C", "D) Option D"],
        "ans": "Correct answer letter",
        "hint": "Hint text here"
    }}
    
    Ensure all 10 questions are unique and cover different aspects of {chapter}.
    """

    try:
        response = model.generate_content(prompt)
        json_str = re.sub(r'```json\n|\n```', '', response.text).strip()
        questions = json.loads(json_str)
        return [Question(**q) for q in questions]
    except json.JSONDecodeError as e:
        logger.error(f"Error parsing JSON: {str(e)}")
        logger.error(f"Raw response: {response.text}")
        raise ValueError("Invalid JSON format in the model's response")
    except Exception as e:
        logger.error(f"Error generating questions: {str(e)}")
        logger.error(f"Raw response: {response.text}")
        raise

@router.get("/generate_questions/{chapter}")
async def get_questions(chapter: str):
    if chapter not in CHAPTERS:
        raise HTTPException(status_code=400, detail="Invalid chapter")

    try:
        questions = generate_questions(CHAPTERS[chapter])
        return JSONResponse(content={
            CHAPTERS[chapter]: {f"question_{i+1}": q.dict() for i, q in enumerate(questions)}
        })
    except Exception as e:
        logger.error(f"Error in get_questions: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to generate questions")