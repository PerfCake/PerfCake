#!/bin/sh
## Release script
git checkout -b release/v$1 develop && \
mvn release:prepare && \
mvn release:perform && \
git checkout develop && \
git merge --no-ff release/v$1 && \
git checkout master && \
git merge --no-ff release/v$1~1 && \
git branch -D release/v$1 && \
git push --all && git push --tags
