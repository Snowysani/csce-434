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
	int funcCounter;

	int numofGlobalVars;

	int memTracker;

	java.util.Map< String, Result > varMap;
	java.util.Map< String, Function > functionMap;

	
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
		functionMap = new java.util.HashMap< String, Function>();
		memTracker = 0;
		funcCounter = 0;
		numofGlobalVars = 0;
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

		// check for any functions
		while (token == scanner.expressionMap.get("function") || token == scanner.expressionMap.get("procedure"))
		{
			int a = defineFunction();
			scanner.Next();
			token = scanner.sym;
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
		}

		// else
		// {
		// 	error();
		// }
		statSequence();

		pushToBuffer(DLX.assemble(RET, 0));

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
				r.isGlobal = true;
				numofGlobalVars++;
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
				//scanner.Next();
				continue;
			}
	
			if (scanner.sym == scanner.expressionMap.get("call")) // if it's a function call
			{
				funcCall();
				continue;
			}
	
			if (scanner.sym == scanner.expressionMap.get("if"))
			{
				ifStatement();
				scanner.Next();
				continue;
			}

			if (scanner.sym == scanner.expressionMap.get("while"))
			{
				
				whileLoop();
				continue;
			}

			if (scanner.sym == scanner.expressionMap.get("return"))
			{
				// do return stuff
				//scanner.Next();
				returnStatement();
				//scanner.Next();
				// If needed, return here, and pass the current sym to the funcProcedure.
				// If current sym == return, continue. Or something/
				continue; 
			}

			if (scanner.sym == scanner.expressionMap.get("fi") 
				|| scanner.sym == scanner.expressionMap.get("else")
				|| scanner.sym == scanner.expressionMap.get("od")
				|| scanner.sym == scanner.expressionMap.get("}"))
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
		
		// if (!varMap.containsKey(myIdent))
		// {
		// 	String name = scanner.Id2String(scanner.id);
		// 	Result r = new Result(name, -1, getNextMemLocation());
		// 	r.isGlobal = false;
		// 	varMap.put(name, r);
		// }

		if (scanner.sym == scanner.expressionMap.get("<-"))
		{
			scanner.Next();
			if (varMap.containsKey(myIdent))
			{
				int expReg = exp();
				Result r = varMap.get(myIdent);
				if (r.isGlobal)
				{
					pushToBuffer(DLX.assemble(STW, expReg, 30, r.address));
				}
				else if (r.isParam)
				{
					// Since it's a parameter, we have to go up the stack via the frame pointer. 
					pushToBuffer(DLX.assemble(STW, expReg, 28, r.localOffset * ( 4)));
				}
				else // r is a local var
				{
					// Since it's local, it lives below the frame pointer. 
					pushToBuffer(DLX.assemble(STW, expReg, 28, - 8 - (r.localOffset * (4))));
				}
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

		int numInstructionsBeforeRelation = bufList.size();

		retRelation rel = relation();

		numInstructionsBeforeRelation = bufList.size() - numInstructionsBeforeRelation;

        if (scanner.sym != scanner.expressionMap.get("then")) error();
		
		scanner.Next();

        rel.offset = statSequence();
        int elseBlockSize = 0; // How many instructions will occur in the else block?

        if (scanner.sym == scanner.expressionMap.get("else")) {
            scanner.Next();

            int currentIndex = bufList.size();
            elseBlockSize = statSequence();

			bufList.add(currentIndex, DLX.assemble(BEQ, 0, elseBlockSize + 1));
        }
		
		bufList.add(rel.idx, DLX.assemble(rel.opcode, rel.regno, bufList.size() - rel.idx - elseBlockSize + 1));
		freeRegister(rel.regno);

        while(scanner.sym != scanner.expressionMap.get("fi"))
		{
			scanner.Next();
		}

        return 0;
	}

	public int whileLoop()
	{
		int ret = 0;
		scanner.Next();
		
		int numInstructionsBeforeRelation = bufList.size();

		retRelation rel = relation();

		numInstructionsBeforeRelation = bufList.size() - numInstructionsBeforeRelation;

		int numInstructionToGoForward = 0;

		if (scanner.sym == scanner.expressionMap.get("do"))
		{
			scanner.Next();
			numInstructionToGoForward += statSequence();
		}              
		if (scanner.sym != scanner.expressionMap.get("od"))
		{
			error();
		}        

		pushToBuffer(rel.idx, DLX.assemble(rel.opcode, rel.regno, numInstructionToGoForward + 2));
		freeRegister(rel.regno);

		pushToBuffer(DLX.assemble(BEQ, 0, (numInstructionsBeforeRelation + numInstructionToGoForward + 1) * -1 )); 

		scanner.Next();
		return ret;
	}

	public int defineFunction()
	{
		if (scanner.sym != scanner.expressionMap.get("function") && scanner.sym != scanner.expressionMap.get("procedure"))
		{
			error();
		}
		Boolean isProcedure = scanner.sym == scanner.expressionMap.get("procedure");
		// Define the function name and get its parameters.
		scanner.Next();
		String funcName = scanner.Id2String(scanner.id);
		scanner.Next();
		token = scanner.sym;

		java.util.Map< String, Result > localVarMap = new java.util.HashMap< String, Result >(); 

		int memBeginLocation = memTracker;

		Function f = new Function();
		f.name = funcName;
		f.isProcedure = isProcedure;
		f.numParams = 0;
		f.numVars = 0;

		// accept the formal params. 
		while (token != 70 && token != scanner.expressionMap.get(")"))
		{
			if (token == 61)
			{
				String name = scanner.Id2String(scanner.id);
				Result r = new Result(name, -1, getNextMemLocation());
				r.isParam = true; // it is a param
				r.isGlobal = false;
				r.functionName = funcName;
				r.localOffset = f.numParams++;
				varMap.put(name, r);
			}
			scanner.Next();
			token = scanner.sym;
		}

		f.returnAddress = getNextMemLocation();
		f.fp = getNextMemLocation();
		f.startInstruction = bufList.size();

		// store the current return address into memory right above the fp 


		// set up the local variables. 
		while (token != 70 && token != scanner.expressionMap.get("{"))
		{
			if (token == 61)
			{
				String name = scanner.Id2String(scanner.id);
				Result r = new Result(name, -1, getNextMemLocation());
				r.isParam = false; // it is a param
				r.isGlobal = false;
				r.functionName = funcName;
				r.localOffset = f.numVars++;
				varMap.put(name, r);
			}
			scanner.Next();
			token = scanner.sym;
		}

		// should have a semicolon
		if (scanner.sym == scanner.expressionMap.get(";"))
		{
			scanner.Next();
		}
		int a = 0;
		if (scanner.sym == scanner.expressionMap.get("{"))
		{
			// do the func body
			int stat = statSequence();
			a += stat;
			//a++;
		}

		if (scanner.sym != scanner.expressionMap.get("}"))
		{
			error();
		}
		f.localVarMap = localVarMap;
		f.numberOfInstructions = a;
		f.memBegin = memBeginLocation;
		functionMap.put(funcName, f);

		// add a branch to the top of these instructions so its skipped.
		//bufList.add(DLX.assemble(BSR, bufList.size() - f.numberOfInstructions));
		int procedureReturn = 0;
		if (f.isProcedure) {
			procedureReturn++;
		}
		bufList.add(bufList.size() - a, DLX.assemble(BEQ, 0, bufList.size() - f.startInstruction + 3 + procedureReturn));
		bufList.add(bufList.size() - a, DLX.assemble(PSH, 31, 29, -4));
		bufList.add(bufList.size() - a, DLX.assemble(PSH, 28, 29, -4));
		// now after all that, initialize the frame pointer i guess or something
		if (isProcedure)
		bufList.add(DLX.assemble(RET, 31));

		bufList.add(DLX.assemble(SUBI, 28, 30, (numofGlobalVars * 4) + (f.numParams * 4) + 4));
		bufList.add(DLX.assemble(SUBI, 29, 30, 4 + numofGlobalVars*4));


		return 0;
	}

	public int functionProcedure()
	{
		// call that function
		String funcName = scanner.Id2String(scanner.id);
		Function f = functionMap.get(funcName);
		if (f == null)
		{
			error();
		}
		scanner.Next();

		// load the current formal input params to their respective mems. 
		
		int paramBeginLocation = f.memBegin;
		// populate those
		scanner.Next(); 
		int i = f.numParams - 1;
		while (scanner.sym != scanner.expressionMap.get(")"))
		{
			if (scanner.sym == scanner.expressionMap.get(","))
			{
				scanner.Next();
				continue;
			}
			int myExp = exp();
			// STW that result
			// TODO: Figure out how to properly push the current expressions to the function formal params memory.
//			bufList.add(DLX.assemble(STW, myExp, 29, f.memBegin + (-4 * (i-1)))); // used to be 30
			bufList.add(DLX.assemble(STW, myExp, 29, (-4 * (i)))); // used to be 30

			i--;
			freeRegister(myExp);
		}
		// okay. now we're at )
		scanner.Next();

		// Branch to that function
		//bufList.add(DLX.assemble(BSR, f.startInstruction +  1));
		// if (f.isProcedure)
		// {
		// 	bufList.add(DLX.assemble(RET, 31)); // want to skip the next JSR. 
		// }
		bufList.add(DLX.assemble(JSR, (f.startInstruction + 1) * 4));

		return 0;
	}

	public int returnStatement()
	{
		scanner.Next();
		int myExp = exp();

		// Register 27 is now my return register.
		//bufList.add(DLX.assemble(ADD, 27, 0, myExp));
		freeRegister(myExp);
		// Push the myExp register to the frame pointer

		bufList.add(DLX.assemble(RET, 31));


		return myExp;
	}

	public retRelation relation()
	{
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
			error();
		}
		freeRegister(exp1);
		freeRegister(exp2);
		return new retRelation(myOp, freeReg, relIndex);
	}

	int exp() {
		int t = term();
		while (scanner.sym == scanner.expressionMap.get("+") || scanner.sym == scanner.expressionMap.get("-")) {
			boolean isMinus = scanner.sym == scanner.expressionMap.get("-");
			scanner.Next();
			if (!isMinus) // if we're substracting or adding.
			{
				int tempRegisterNumber = term();
				pushToBuffer(DLX.assemble(ADD, t, t, tempRegisterNumber));
				freeRegister(tempRegisterNumber);
			}
			if (isMinus)
			{
				int tempRegisterNumber = term();
				pushToBuffer(DLX.assemble(SUB, t, t, tempRegisterNumber));
				freeRegister(tempRegisterNumber);
			}
		}
		return t;
	}

	int term() {
		int t = factor(); // TODO: if scanner.sym is a number, return the value rather than the register.
		boolean multiply = false;
		while (scanner.sym == scanner.expressionMap.get("*") || scanner.sym == scanner.expressionMap.get("/")) {
			if (scanner.sym == scanner.expressionMap.get("*")) multiply = true;
			scanner.Next();
			int factorReg = factor();
			if (multiply)
				pushToBuffer( DLX.assemble(MUL, t, t, factorReg) );
			else // divide
				pushToBuffer( DLX.assemble(DIV, t, t, factorReg) );
			freeRegister(factorReg);
		}
		return t;
	}

	int factor() {
		int ret = 0;
		if (scanner.sym == 60) // is a number
		{
			// ADDI to a register.
			int nextReg = getNextReg();
			pushToBuffer(DLX.assemble(ADDI, nextReg, 0, scanner.val));
			ret = nextReg; // return the location of the register.
			scanner.Next();
		}
		else if (scanner.sym == 61)
		{	// var identity
			if ( varMap.get(scanner.Id2String(scanner.id)) == null)  // if we do not have that variable.
			{
				//TODO: relook at this code.
				// // add it to the reg map
				// String name = scanner.Id2String(scanner.id);
				// Result r = new Result(name, -1, getNextMemLocation());
				// varMap.put(name, r);
			}
		
			Result r = varMap.get(scanner.Id2String(scanner.id));
			ret = getNextReg();
			// LDW the value in memory to the register.
			//Function f = functionMap.get(r.functionName);
			if (r.isGlobal)
			{
				pushToBuffer(DLX.assemble(LDW, ret, 30, r.address));
			}
			else if (r.isParam) // go up 
			{
				pushToBuffer(DLX.assemble(LDW, ret, 28, (r.localOffset + 1) * (4)));
			}
			else // is a local var
			{
				pushToBuffer(DLX.assemble(LDW, ret, 28, -8 + (r.localOffset) * (-4)));
			}

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
			if (ret != 0) freeRegister( ret );
			if (ret == 0) return mostRecentlyUsedReg;
		}
		else
		{
			ret = 0; //error();
		}
		return ret;
	}

	int funcCall() { // assume we are at a "call" reference.
		scanner.Next();
		if (scanner.sym != 61)
			error();
		// funcCall = "call" ident [ "(" [ expression  {" ," expression } ] ")" ].
		String myIdent = scanner.Id2String(scanner.id);
		if (myIdent.equals("inputnum"))
		{
			int reg = getNextReg();
			pushToBuffer(DLX.assemble(RDI, reg)); // read input value
			freeRegister(reg);
			return reg;
		}
		if (myIdent.equals("outputnum"))
		{
			scanner.Next();
			int myExpression = exp();

			pushToBuffer(DLX.assemble(51, myExpression));
			freeRegister(myExpression);

			return myExpression;
		}
		if (myIdent.equals("outputnewline"))
		{
			pushToBuffer(DLX.assemble(53)); // write new line opcode
		}
		if (functionMap.get(myIdent) != null)
		{
			// store all the registers in use 
			functionProcedure();
			/*
			right before calling a function you store the current return address (r31) into memory right above the fp
			then when you branch to the function with bsr
			the return address register will be automatically updated to return after the bsr instruction
			*/
			//Function f = functionMap.get(myIdent);
			bufList.add(DLX.assemble(POP, 31, 29, 4));
			bufList.add(DLX.assemble(POP, 28, 29, 4));
			// Store the current return address (R31) into memory right above FP (R28)
			
			// Branch to the function with BSR
			// bufList.add(DLX.assemble(BSR, -f.numberOfInstructions));
			// return address will be automatically updated to return after the BSR instruction.
			return mostRecentlyUsedReg;
		}
		return 0;
	}

	int getNextMemLocation() {
		int j = memTracker;
		memTracker += -4;
		//j += -4; // multiply by four for memory mapping
		return memTracker;
	}

class Result {
	// from class whiteboard
	int regno;
	int address;
	int value;
	String name;
	String functionName;
	Boolean isParam;
	Boolean isGlobal;
	int localOffset;

	Result(String _name, int reg, int mem) {
		regno = reg;
		address = mem;
		name = _name;
		functionName = "main";
	}
	Result(String _name, int reg, int mem, String _functionName) {
		regno = reg;
		address = mem;
		name = _name;
		functionName = _functionName;
	}

	Result() {
		regno = 0; address = 0; name = ""; functionName = "";
	}
}

class Function {
	int numberOfInstructions;
	String name;
	int memBegin;
	java.util.Map< String, Result > localVarMap;
	int returnAddress;
	int fp;
	int startInstruction;
	Boolean isProcedure;
	int numParams;
	int numVars;
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