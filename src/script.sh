#!/bin/sh

gnome-terminal --geometry=100x30 -- /bin/sh -c 'javac Coordinator.java; java Coordinator 1243 4' &
sleep 5
gnome-terminal --geometry=80x10 -- /bin/sh -c 'javac Participant.java; java Participant 1243 2222' &
sleep 3
gnome-terminal --geometry=80x10 -- /bin/sh -c 'javac Participant.java; java Participant 1243 3333' &
sleep 3
gnome-terminal --geometry=80x10 -- /bin/sh -c 'javac Participant.java; java Participant 1243 4444' &
sleep 3
gnome-terminal --geometry=80x10 -- /bin/sh -c 'javac Participant.java; java Participant 1243 5555'

rm *.class