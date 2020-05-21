#!/bin/sh

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; javac Coordinator.java; java Coordinator 12345 12344 4 500 A B C"'
sleep 6

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; javac Participant.java; java Participant 12345 12344 2222 555"'
sleep 6

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; java Participant 12345 12344 4444 555"'
sleep 6

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; java Participant 12345 12344 3333 555"'
sleep 6

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; java Participant 12345 12344 5555 555"'

rm *.class
