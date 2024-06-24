import os
from fastapi import FastAPI, HTTPException
from firebase_admin import credentials, initialize_app, storage

app = FastAPI()
os.environ["FIREBASE_ADMIN_SDK"] = r"firebase_cred.json"
cred = credentials.Certificate(os.environ["FIREBASE_ADMIN_SDK"])
firebase_app = initialize_app(cred, {
    'storageBucket': 'penny-89979.appspot.com'
})
firebase_storage = storage.bucket()

@app.get("/get-file/{folder}/{subfolder}/{filename}")
async def get_file(folder: str, subfolder: str, filename: str):
    try:
        file_path = f"{folder}/{subfolder}/{filename}"
        blob = firebase_storage.blob(file_path)
        content = blob.download_as_text()

        if content:
            return {"content": content}
        else:
            raise HTTPException(status_code=404, detail="File not found")
    except HTTPException as e:
        raise e
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
