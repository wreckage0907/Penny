from fastapi import APIRouter, HTTPException
from fastapi.responses import JSONResponse
import google.generativeai as genai
from schema.question import Question,CHAPTERS
import os
from dotenv import load_dotenv

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

def generate_question(chapter):
    try:
        prompt = f"""
        Generate a finance question related to {CHAPTERS[chapter]} with 4 options, the correct answer, and a hint.
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
        """

        model = genai.GenerativeModel('gemini-1.5-flash')
        response = model.generate_content(prompt)

        lines = response.text.strip().split('\n')
        question_text = next((line.split(': ', 1)[1] for line in lines if line.startswith("Question:")), "")
        options = [line.split(') ', 1)[1] for line in lines if line.strip().startswith(('A)', 'B)', 'C)', 'D)'))]
        correct_answer = next((line.split(': ', 1)[1] for line in lines if line.startswith("Answer:")), "")
        hint = next((line.split(': ', 1)[1] for line in lines if line.startswith("Hint:")), "")

        if not question_text or len(options) != 4 or not correct_answer or not hint:
            raise ValueError("Invalid response format from model")

        return Question(
            question=question_text,
            options=options,
            ans=correct_answer,
            hint=hint
        )

    except Exception as err:
        raise ValueError(f"Failed to generate question: {str(err)}")
    
@router.get("/generate_questions/{chapter}")
async def generate_questions(chapter: str):
    if chapter not in CHAPTERS:
        raise HTTPException(status_code=400, detail="Invalid chapter")
    questions = {}
    for i in range(5):
        try:
            question = generate_question(chapter)
            questions[f"question_{i+1}"] = question.dict()
        except Exception as err:
            continue  

    if not questions:
        raise HTTPException(status_code=500, detail="Failed to generate any questions")

    return JSONResponse(content={CHAPTERS[chapter]: questions})