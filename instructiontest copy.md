0: BEQ  0 21            // Start at instruction 21.
1: PSH  31 29 -4
2: PSH  28 29 -4
3: LDW  1 28 4
4: ADDI  2 0 1
5: SUB  3 1 2
6: BNE  3 4
7: ADDI  1 0 1
8: ADD  27 0 1
9: RET  31
10: LDW  1 28 4
11: ADDI  2 0 1
12: SUB  1 1 2
13: STW  1 29 0
14: JSR  4
15: POP  31 29 4
16: POP  28 29 4
17: LDW  1 28 4
18: ADD  27 27 1
19: ADD  27 0 27
20: RET  31
21: SUBI  28 30 16      // 
22: SUBI  29 30 12      // 
23: RDI  1
24: STW  1 30 -4
25: LDW  1 30 -4
26: STW  1 29 0
27: JSR  4
28: POP  31 29 4
29: POP  28 29 4
30: STW  27 30 -8
31: WRL  
32: LDW  1 30 -4
33: WRD  1
34: WRL  
35: LDW  1 30 -8
36: WRD  1
37: WRL  
38: RET  0