#!/usr/bin/env bash
set -euox pipefail
# assumes that dotty/scala/compiler-benchmark are sibling directories.
# It's necessary to pull in the scala/dotty repos in order to fetch the latest
# commit messages.
gitrepos=${1:-$HOME}
cd $gitrepos/dotty && git checkout master && git pull origin master
cd $gitrepos/scala && git checkout 2.13.x && git pull origin 2.13.x
cd $gitrepos/compiler-benchmark && git pull origin dotty && sbt -Dgitrepos=$gitrepos runBatch
