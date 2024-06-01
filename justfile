jar-build:
    sbt --client assembly

docker-build:
    just jar-build
    docker build . -t rpimon-local

docker-run:
    docker run -it --network=host --env=RPIMON_TICK=0.seconds --rm rpimon-local

docker-size:
    docker inspect  -f "{{{{ .Size }}" rpimon-local | numfmt --to=si

test:
    sbt --client test

mqtt-run:
    docker-compose up

test-run:
    sbt --client Test/run
