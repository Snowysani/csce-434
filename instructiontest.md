0: BEQ  0 11       // Branch to Instruction 11 - the SUBI
1: PSH  31 29 -4   // 
2: PSH  28 29 -4
3: RDI  1          // Read "444" into Register 1.
4: STW  1 28 -8    // Store "444" into location 9975, local var i memory location.
5: LDW  1 28 -8    // Load "444" into Register 1.
6: LDW  2 28 4     // Load "555" into register 2, 28 is currently 9983 + 4 is 9987, the formal params memory location.
7: ADD  1 1 2      // 1 =: 444 + 555
8: STW  1 28 -12   // 1 =: 9971, which is the local location for k. K is now "999"
9: LDW  1 28 -12   // Load k into register 1. Maybe load it into register 27, set up a dedicated return value reg?
10: RET  31        // Go back to instruction 18.
11: SUBI  28 30 16 // Stack Pointer becomes 9983
12: SUBI  29 30 12 // Frame pointer becomes 9987
13: RDI  2         // Read "555" into register 2. 
14: STW  2 30 -4   // Store that into global var location 1, which is 9995
15: LDW  2 30 -4   // Load "555" into register 2. 
16: STW  2 29 0    // Store "555" to the memory location at the frame pointer which is 9987, the formal params memory location for gettwosum.
17: JSR  4         // Jump to instruction 1
18: POP  31 29 4   // 
19: POP  28 29 4
20: STW  2 30 -8   // Again, we load the location of myvar into register 2. This is wrong. Myvar is never updated with the return value.
21: LDW  2 30 -8
22: WRD  2
23: WRL  
24: RET  0