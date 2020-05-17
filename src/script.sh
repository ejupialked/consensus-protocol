#!/bin/sh

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; javac Coordinator.java; java Coordinator 1243 4"'
sleep 3

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; javac Participant.java; java Participant 1243 2222"'
sleep 3

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; javac Participant.java; java Participant 1243 4444"'
sleep 3

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; javac Participant.java; java Participant 1243 3333"'
sleep 3

osascript -e 'tell app "Terminal" to do script "cd /Users/alked/IdeaProjects/consensus-protocol/src; javac Participant.java; java Participant 1243 5555"'

rm *.class
