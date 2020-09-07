# Consensus protocol - A Series of Indicative Votes
The aim of this assignment was to implement a consensus protocol that tolerates participant
failures. The protocol involves two types of processes: a coordinator, whose role is to initiate a run of
the consensus algorithm and collect the outcome of the vote; and a participant, which contributes a
vote and communicates with the other participants to agree on an outcome. The application should
handle 1 coordinator process and N participant processes, out of which any number of participants
may fail during the run of the consensus algorithm. The actual consensus algorithm is run among the
participant processes, with the coordinator only collecting outcomes from the participants. 


A more detailed description of the assignment can be found [here](https://github.com/ejupialked/consensus-protocol/blob/master/spec.pdf).

Mark: 83%

## Author
Alked Ejupi Copyright (2020). All rights reserved.

## Reference
Coulouris, Dollimore and Kindberg, Distributed Systems - Concepts and Design, 5th edition, Addison Wesley, 2011.

