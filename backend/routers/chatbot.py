import logging
import os
from dotenv import load_dotenv
from fastapi import APIRouter, HTTPException
import google.generativeai as genai
from sentence_transformers import SentenceTransformer
import faiss
import fitz  
from typing import List, Dict
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
load_dotenv()
MODEL_NAME = 'gemini-1.5-pro'
SENTENCE_MODEL_NAME = 'all-MiniLM-L6-v2'
PDF_PATH = 'routers/Gemini Contest.pdf' 
TOP_K = 3
api_key = os.getenv('GEMINI_API_KEY')
if not api_key:
    raise ValueError("API key not found. Please set the GEMINI_API_KEY environment variable.")
genai.configure(api_key=api_key)
model = genai.GenerativeModel(MODEL_NAME)
sentence_model = SentenceTransformer(SENTENCE_MODEL_NAME)
class ChatRequest(BaseModel):
    user_id: str
    message: str

class ChatResponse(BaseModel):
    response: str
    history: List[str]

conversation_history: Dict[str, List[str]] = {}
finance_info: List[str] = [
    "Personal finance refers to managing one's financial resources effectively, including earning, spending, saving, investing, and protecting against risks.",
    "Budgeting is a crucial aspect of personal finance, helping individuals track income and expenses.",
    "Investing involves allocating resources with the expectation of generating future returns.",
    "Risk management in finance includes strategies to mitigate potential financial losses.",
    "Retirement planning is an essential component of long-term financial security.",
    "Credit management involves responsibly using and maintaining good credit scores.",
    "Tax planning strategies can help optimize an individual's financial situation.",
    "Estate planning ensures the proper distribution of assets after one's death.",
    "Emergency funds are savings set aside for unexpected financial needs.",
    "Financial literacy education empowers individuals to make informed financial decisions."
]

def extract_text_from_pdf(file_path: str) -> str:
    with fitz.open(file_path) as document:
        text = ""
        for page in document:
            text += page.get_text()
    return text

pdf_content = extract_text_from_pdf(PDF_PATH)
finance_info.append(pdf_content)
finance_embeddings = sentence_model.encode(finance_info)
dimension = finance_embeddings.shape[1]
index = faiss.IndexFlatL2(dimension)
index.add(finance_embeddings.astype('float32'))

router = APIRouter()

def get_history(user_id: str) -> List[str]:
    return conversation_history.get(user_id, [])

def get_relevant_context(query: str, top_k: int = TOP_K) -> str:
    query_vector = sentence_model.encode([query])
    distances, indices = index.search(query_vector.astype('float32'), top_k)
    relevant_points = [finance_info[i] for i in indices[0]]
    return " ".join(relevant_points)

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest) -> ChatResponse:
    try:
        context = get_relevant_context(request.message)
        prompt = f"Based on the following context from our finance course: '{context}', please answer the user's question: {request.message}"
        
        response = model.generate_content(prompt)
        response_text = response.text

        if request.user_id not in conversation_history:
            conversation_history[request.user_id] = []
        conversation_history[request.user_id].append(f"User: {request.message}")
        conversation_history[request.user_id].append(f"Bot: {response_text}")

        history = get_history(request.user_id)
        return ChatResponse(response=response_text, history=history)

    except Exception as e:
        logger.error(f"An error occurred: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
