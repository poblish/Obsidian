rm -f dist/*

docker run --rm --name obsidian-docker-build \
  -v ~/.m2:/root/.m2 \
  -v "$PWD":/usr/src/mymaven \
  -w /usr/src/mymaven maven \
  mvn clean package -Dmaven.test.skip
