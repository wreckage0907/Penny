FROM python:3.10-slim

ENV PYTHONUNBUFFERED True
ENV APP_HOME /app
ENV PORT 8000

WORKDIR $APP_HOME

RUN adduser --system --group app_user

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN chown -R app_user:app_user $APP_HOME

USER app_user

HEALTHCHECK CMD curl --fail http://localhost:$PORT/health || exit 1

CMD exec uvicorn main:app --host 0.0.0.0 --port $PORT --workers 1