#!/bin/bash

USAGE="Usage: $0 [-n] [-b] repo_directory\n Use -b to batch all events (adds a delay of at least 0.1s before processing the index)\n Use -n to not parse dependencies"

DIR=`dirname $0`
DO_BATCH=0
CREATE_OPTS=""
INDEX_NAME="index.gz"

while (( "$#" )); do
    case "$1" in
        -h)
            echo -e $USAGE
            exit 1
            ;;
        -b)
            DO_BATCH=1
            ;;
        -n)
            echo "Won't parse dependencies of artifacts"
            CREATE_OPTS="-n -m"
            ;;
        *)
            ARTIFACTS=$1
            ;;
    esac
    shift
done

trap 'kill $(jobs -p)' EXIT

#create index before watching on events
python $DIR/create_index.py $CREATE_OPTS $ARTIFACTS $ARTIFACTS/$INDEX_NAME;
inotifywait -m -e close_write,moved_from,moved_to,delete $ARTIFACTS \
| grep -v $INDEX_NAME --line-buffered \
| while read line; do
    # read all other incoming events with a short timeout to process
    # all of them together
    if [ $DO_BATCH -eq 1 ]; then
        while read -t 0.1 line; do true; done;
    fi
    python $DIR/create_index.py $CREATE_OPTS $ARTIFACTS $ARTIFACTS/$INDEX_NAME;
done
