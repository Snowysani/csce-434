package edu.tamu.csce434;

import java.util.List;
import java.util.ArrayList;

public class Compiler 
{
	private edu.tamu.csce434.Scanner scanner;
	
	int buf[] = new int[DLX.MemSize/4 - 1];		
	List<Integer> bufList = new ArrayList<Integer>(); 
	int R[] = new int[32]; // register array
	void freeRegister(int i)
	{
		R[i] = 0;
	}
	int mostRecentlyUsedReg;

	int bufferPointer;

	private int token;

	java.util.Map< String, Result > varMap;
	
	// Constructor of your Compiler
	public Compiler(String args)
	{
		scanner = new Scanner(args);
		R[0] = 0;
		for (int i = 1; i < 27; i++)
		{
			R[i] = 0;
		} // populate the registers
		R[30] = DLX.MemSize - 1;
		bufferPointer = 0; mostRecentlyUsedReg = 1;
		varMap = new java.util.HashMap< String, Result >();
	}
	
	
	// Implement this function to start compiling your input file
	public int[] getProgram()  
	{	
		token = scanner.sym;
		if (token != scanner.expressionMap.get("main"))
		{
			//its not main, output error.
			error();
		}

		if (token == scanner.expressionMap.get("main"))
		{
			// check for varDec
			scanner.Next();
			token = scanner.sym;
			if (token == scanner.expressionMap.get("var"))
			{
				// then its a var. 
				varDec();
			}
		}

		// at this point we should have a semicolon
		if (token == scanner.expressionMap.get(";"))
		{
			scanner.Next();
			token = scanner.sym;
		}

		// now we should have an open {
		if (token == scanner.expressionMap.get("{"))
		{
			scanner.Next();
			token = scanner.sym;
		}
		else
		{
			error();
		}
		statSequence();

		// stream arraylist to the buffer.
		buf = bufList.stream().mapToInt(i -> i).toArray();

		scanner.closefile();
		
		return buf;
	}

	private int getNextReg()
	{
		for (int i = 1; i < 27; i++)
		{
			if (R[i] == 0)
			{
				mostRecentlyUsedReg = i;
				R[i] = 1;
				return i;
			}
		}
		return -1; // no free registers
	}

	private void pushToBuffer(int idx, int inst)
	{
		bufList.add(idx, inst);
		//buf[bufferPointer++] = inst;
	}

	private void pushToBuffer(int inst)
	{
		bufList.add(inst);
	}

	private void error()
	{
		System.out.println("error");
	}
	
	public void varDec() 
	{
		scanner.Next();
		token = scanner.sym;
		while (token != 70)
		{
			if (token == 61)
			{
				String name = scanner.Id2String(scanner.id);
				Result r = new Result(name, -1, getNextMemLocation());
				varMap.put(name, r);
			}
			else if (token == 31) // if its a comma, continue
			{
				scanner.Next();
				token = scanner.sym;
				continue;
			}
			scanner.Next();
			token = scanner.sym;
		}
	}

	public int statSequence()
	{
		int i1 = bufList.size();
		while (scanner.sym != scanner.expressionMap.get(".") && scanner.sym != 255)
		{
			if (scanner.sym == scanner.expressionMap.get("let")) // if it's an assignment
			{
				assignment();
				scanner.Next();
				continue;
			}
	
			if (scanner.sym == scanner.expressionMap.get("call")) // if it's a function call
			{
				int reg = funcCall();
				freeRegister(reg);
				//scanner.Next();
				continue;
			}
	
			if (scanner.sym == scanner.expressionMap.get("if"))
			{
				int reg = ifStatement();
				freeRegister(reg);
				scanner.Next();
				continue;
			}

			if (scanner.sym == scanner.expressionMap.get("while"))
			{
				
				whileLoop();
				//scanner.Next();
				continue;
			}

			if (scanner.sym == scanner.expressionMap.get("fi") 
				|| scanner.sym == scanner.expressionMap.get("else")
				|| scanner.sym == scanner.expressionMap.get("od"))
			{
				return bufList.size() - i1;
			}

			if (scanner.sym == scanner.expressionMap.get(";"))
			{
				scanner.Next();
				continue;
			}

			if (scanner.sym == 255)
			{
				break;
			}

			scanner.Next();
		}
		// now we are at the end of file.
		pushToBuffer(DLX.assemble(RET, 0));
		return bufList.size() - i1;
	}

	public void assignment()
	{
		scanner.Next();
		if (scanner.sym != 61) // is not ident
		{
			error();
		}
		String myIdent = scanner.Id2String(scanner.id);
		scanner.Next();
		
		if (!varMap.containsKey(myIdent))
		{
			String name = scanner.Id2String(scanner.id);
			Result r = new Result(name, -1, getNextMemLocation());
			varMap.put(name, r);
		}

		if (scanner.sym == scanner.expressionMap.get("<-"))
		{
			scanner.Next();
			if (varMap.containsKey(myIdent))
			{
				int expReg = exp();
				Result r = varMap.get(myIdent);
				// STW that register value in the variable's memory location.
				pushToBuffer(DLX.assemble(STW, expReg, 30, r.address));
				freeRegister(expReg);
			}
			else // its not in the var map
			{
				error();
			}
		}
	}

	public int ifStatement()
	{
		scanner.Next();
		retRelation rel = relation();

        if (rel == null) error();
		if (scanner.sym != scanner.expressionMap.get("then")) error();

		scanner.Next();

        rel.offset = statSequence();
        int numElseInstructions = 1;
        
        if (scanner.sym == scanner.expressionMap.get("else")) {
            scanner.Next();
            int finishIndex = bufList.size();
            int numElseStuff = statSequence();
			bufList.add(finishIndex, DLX.assemble(40, 0, numElseStuff + 1));
            numElseInstructions += numElseStuff;
        }

        bufList.add(rel.idx + 1, DLX.assemble(rel.opcode, rel.regno, rel.offset + numElseInstructions + 1)); 

        while(scanner.sym != scanner.expressionMap.get("fi"))
		{
			scanner.Next();
		}

        //freeRegister(r.reg);
        return rel.regno;
	}

	public int whileLoop()
	{
		int ret = 0;
		scanner.Next();
		retRelation rel = relation();

		int offset = 0;
		if (scanner.sym == scanner.expressionMap.get("do"))
		{
			scanner.Next();
			offset = statSequence();
		}              
		if (scanner.sym != scanner.expressionMap.get("od"))
		{
			error();
		}        
		pushToBuffer(rel.idx, DLX.assemble(rel.opcode, rel.regno, offset + 2));   

		pushToBuffer(DLX.assemble(BEQ, 0, (offset + 3) * -1 )); 

		scanner.Next();
		ret = 0;

		freeRegister(rel.regno);
		return ret;
	}

	public retRelation relation()
	{
		int ret;
		int exp1 = exp();
		int op = scanner.sym;
		int myOp = 0;
		scanner.Next();
		int exp2 = exp();

		int freeReg = getNextReg();

		pushToBuffer(DLX.assemble(SUB, freeReg, exp1, exp2));
		int relIndex = bufList.size();

		if (op == scanner.expressionMap.get("=="))
		{
			myOp = BNE;
		}
		else if (op == scanner.expressionMap.get("!="))
		{
			myOp = BEQ;
		}
		else if (op == scanner.expressionMap.get("<"))
		{
			myOp = BGE;
		}
		else if (op == scanner.expressionMap.get("<="))
		{
			myOp = BGT;
		}
		else if (op == scanner.expressionMap.get(">"))
		{
			myOp = BLE;
		}
		else if (op == scanner.expressionMap.get(">="))
		{
			myOp = BLT;
		}
		else
		{
			ret = 0;
			error();
		}
		freeRegister(exp1);
		freeRegister(exp2);
		return new retRelation(myOp, freeReg, relIndex);
	}

	int exp() {
		int ret; 
		int t = term();
		int nextReg = getNextReg();
		boolean hitFlag = false;
		while (scanner.sym == scanner.expressionMap.get("+") || scanner.sym == scanner.expressionMap.get("-")) {
			hitFlag = true;
			boolean isMinus = scanner.sym == scanner.expressionMap.get("-");
			scanner.Next();
			if (!isMinus) // if we're substracting or adding.
			{
				int tempRegisterNumber = term();
				pushToBuffer(DLX.assemble(ADD, nextReg, t, tempRegisterNumber));
				freeRegister(tempRegisterNumber);		
				freeRegister(t);

			}
			if (isMinus)
			{
				int tempRegisterNumber = term();
				pushToBuffer(DLX.assemble(SUB, nextReg, t, tempRegisterNumber));
				freeRegister(tempRegisterNumber);
				freeRegister(t);
			}
		}
		if (hitFlag) {
			return nextReg;
		}
		else {
			freeRegister(nextReg);
			ret = t; 
			return ret;
		}
	}

	int term() {
		int ret;
		
		int t = factor();
		int nextReg = getNextReg();
		boolean hitFlag = false;

		while (scanner.sym == scanner.expressionMap.get("*")) {
			hitFlag = true;
			scanner.Next();
			//t *= factor(); // use MULT maybe?
			int factorReg = factor();
			pushToBuffer( DLX.assemble(MUL, nextReg, t, factorReg) );
			freeRegister(t);
			freeRegister(factorReg);
		}
		while (scanner.sym == scanner.expressionMap.get("/")) {
			hitFlag = true;
			scanner.Next();
			//t /= factor(); // maybe we can DIV here
			int factorReg = factor();
			pushToBuffer( DLX.assemble(DIV, nextReg, t, factorReg) );
			freeRegister(t);
			freeRegister(factorReg);
		}
		if (hitFlag)
		{
			ret = nextReg;
		}
		else
		{
			ret = t;
		}
		return ret;
	}

	int factor() {
		int ret = 0;
		if (scanner.sym == 60) // is a number
		{
			// ADDI to a register.
			int nextReg = getNextReg();
			pushToBuffer(DLX.assemble(ADDI, nextReg, 0, scanner.val));
			ret = nextReg; // return the location of the register.
			//freeRegister(ret);
			scanner.Next();
		}
		else if (scanner.sym == 61)
		{	// var identity
			if ( varMap.get(scanner.Id2String(scanner.id)) == null)  // if we do have that variable.
			{
				// add it to the reg map
				String name = scanner.Id2String(scanner.id);
				Result r = new Result(name, -1, getNextMemLocation());
				varMap.put(name, r);
			}
		
			Result r = varMap.get(scanner.Id2String(scanner.id));
			if (r.regno == -1) r.regno = getNextReg();
			
			// LDW the value in memory to the register.
			pushToBuffer(DLX.assemble(LDW, r.regno, 30, r.address));

			ret = r.regno;
			//freeRegister(ret);
			scanner.Next();
		}
		else if (scanner.sym == scanner.expressionMap.get("("))
		{	// expression
			scanner.Next();
			ret = exp(); // return the next register value.
			if (scanner.sym == scanner.expressionMap.get(")"))
			{
				scanner.Next();
			}
		}
		else if (scanner.sym == scanner.expressionMap.get("call"))
		{
			ret = funcCall();
		}
		else
		{
			ret = 0; //error();
		}
		return ret;
	}

	int funcCall() { // assume we are at a "call" reference.
		int ret;
		scanner.Next();
		if (scanner.sym != 61)
			error();
		// funcCall = "call" ident [ "(" [ expression  {" ," expression } ] ")" ].
		String myIdent = scanner.Id2String(scanner.id);
		if (myIdent.equals("inputnum"))
		{
			int nextReg = getNextReg();
			pushToBuffer(DLX.assemble(RDI, nextReg)); // read input value
			//freeRegister(nextReg);
			return nextReg;
		}
		if (myIdent.equals("outputnum"))
		{
			scanner.Next();
			if (scanner.sym == 61 && !varMap.containsKey(scanner.Id2String(scanner.id)))
				return 1;
			int myExpression = exp();
			//outputNum(exp());
			pushToBuffer(DLX.assemble(51, myExpression));
			return myExpression;
			//freeRegister(myExpression);
		}
		if (myIdent.equals("outputnewline"))
		{
			//outputnewline();
			pushToBuffer(DLX.assemble(53)); // write new line opcode
		}
		ret = 0;
		return ret;
	}

	int getNextMemLocation() {
		int j = varMap.size() + 1;
		j *= -4; // multiply by four for memory mapping
		return j;
	}

	static final int ADD = 0;  
	static final int SUB = 1;
	static final int MUL = 2;
	static final int DIV = 3;
	static final int MOD = 4;
	static final int CMP = 5;
	static final int OR  = 8;
	static final int AND = 9;
	static final int BIC = 10;
	static final int XOR = 11;
	static final int LSH = 12;
	static final int ASH = 13;
	static final int CHK = 14;

	static final int ADDI = 16;
	static final int SUBI = 17;
	static final int MULI = 18;
	static final int DIVI = 19;
	static final int MODI = 20;
	static final int CMPI = 21;
	static final int ORI  = 24;
	static final int ANDI = 25;
	static final int BICI = 26;
	static final int XORI = 27;
	static final int LSHI = 28;
	static final int ASHI = 29;
	static final int CHKI = 30;

	static final int LDW = 32;
	static final int LDX = 33;
	static final int POP = 34;
	static final int STW = 36;
	static final int STX = 37;
	static final int PSH = 38; 

	static final int BEQ = 40;
	static final int BNE = 41;
	static final int BLT = 42;
	static final int BGE = 43;
	static final int BLE = 44;
	static final int BGT = 45;
	static final int BSR = 46;
	static final int JSR = 48;
	static final int RET = 49;

	static final int RDI = 50;
	static final int WRD = 51;
	static final int WRH = 52;
	static final int WRL = 53;
	
	static final int ERR = 63; // error opcode which is insertered by loader 
	                           // after end of program code
}

class Result {
	// from class whiteboard
	int regno;
	int address;
	int value;
	String name;

	Result(String _name, int reg, int mem) {
		regno = reg;
		address = mem;
		name = _name;
	}

	Result() {
		regno = 0; address = 0; name = "";
	}
}

class retRelation {
    int opcode;
    int regno;
    int idx;
    int offset;
    
    retRelation() {
        opcode = -1;
        regno = -1;
        idx = 0;
        offset = 0;
    }

    retRelation(int op, int r, int i) {
        opcode = op;
        regno = r;
        idx = i;
        offset = 0;
    }
}