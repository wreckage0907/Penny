from fastapi import APIRouter, HTTPException
from fastapi.responses import JSONResponse
import google.generativeai as genai
from schema.question import Question, CHAPTERS
import os
from dotenv import load_dotenv
import hashlib
import time

load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
try:
    genai.configure(api_key=api_key)
    model = genai.GenerativeModel('gemini-1.5-flash')
    test_response = model.generate_content("Test")
    if not test_response.text:
        raise Exception("Invalid API key or model access")
except Exception as e:
    raise Exception(f"Error configuring Gemini API: {str(e)}")

router = APIRouter()

def hash_question(question_text):
    return hashlib.md5(question_text.encode()).hexdigest()

def generate_question(chapter, existing_questions):
    max_attempts = 5  # Increased max attempts to account for potential duplicates
    for attempt in range(max_attempts):
        try:
            prompt = f"""
            Generate a finance question related to {CHAPTERS[chapter]} with 4 options, ensuring only one correct answer.
            The question should be suitable for someone learning about personal finance.
            Format:
            Question: [question text]
            Options:
            A) [option A]
            B) [option B]
            C) [option C]
            D) [option D]
            Answer: [correct option letter]
            Hint: [hint text]
            
            Important: Ensure that only one option is correct. The other options should be plausible but clearly incorrect.
            """

            response = model.generate_content(prompt)

            lines = response.text.strip().split('\n')
            question_text = next((line.split(': ', 1)[1] for line in lines if line.startswith("Question:")), "")
            options = [line.split(') ', 1)[1] for line in lines if line.strip().startswith(('A)', 'B)', 'C)', 'D)'))]
            correct_answer = next((line.split(': ', 1)[1] for line in lines if line.startswith("Answer:")), "")
            hint = next((line.split(': ', 1)[1] for line in lines if line.startswith("Hint:")), "")

            if not question_text or len(options) != 4 or not correct_answer or not hint:
                raise ValueError("Invalid response format from model")

            # Check for duplicate question
            question_hash = hash_question(question_text)
            if question_hash in existing_questions:
                raise ValueError("Duplicate question generated")

            # Check for multiple correct answers
            if sum(1 for option in options if option.lower().startswith("correct") or option.lower().startswith("true")) > 1:
                raise ValueError("Multiple correct answers detected")

            return Question(
                question=question_text,
                options=options,
                ans=correct_answer,
                hint=hint
            ), question_hash

        except Exception as err:
            if attempt == max_attempts - 1:
                raise ValueError(f"Failed to generate question after {max_attempts} attempts: {str(err)}")
            time.sleep(2 ** attempt)
    
    raise ValueError("Unexpected error in question generation")

@router.get("/generate_questions/{chapter}")
async def generate_questions(chapter: str, num_questions: int = 5):
    if chapter not in CHAPTERS:
        raise HTTPException(status_code=400, detail="Invalid chapter")
    
    if num_questions < 1 or num_questions > 10:
        raise HTTPException(status_code=400, detail="Number of questions must be between 1 and 10")

    questions = {}
    existing_questions = set()
    failures = 0
    max_failures = num_questions * 3  # Allow up to triple the number of attempts

    while len(questions) < num_questions and failures < max_failures:
        try:
            question, question_hash = generate_question(chapter, existing_questions)
            question_number = len(questions) + 1
            questions[f"question_{question_number}"] = question.dict()
            existing_questions.add(question_hash)
        except Exception as err:
            failures += 1
            time.sleep(1)  # Add a small delay between retries

    if not questions:
        raise HTTPException(status_code=500, detail="Failed to generate any questions")

    if len(questions) < num_questions:
        return JSONResponse(content={
            "warning": f"Only generated {len(questions)} out of {num_questions} requested questions",
            CHAPTERS[chapter]: questions
        })

    return JSONResponse(content={CHAPTERS[chapter]: questions})