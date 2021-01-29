FROM python:3.8
COPY . /statsd
WORKDIR /statsd
ENV PYTHONUNBUFFERED=1
CMD ["python", "webserver.py"]
