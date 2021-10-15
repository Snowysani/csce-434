package edu.tamu.csce434;
public class Parser 
{
	private Scanner scanner;
	private int inputNumber;	// stores result when calling inputnum()
	private int token;
	private Scanner inputNumScanner;
	private int currentIdentId;

	java.util.Map< String, Integer > varMap;
	java.util.Map< String, Integer > valueMap;

	// Use this function to print errors, i is symbol/token value
	private void printError()//(int i) 
	{
		System.out.println("Usage: java Parser testFileToScan dataFileToRead");
	}
	
	
	// Constructor of your Parser
	public Parser(String args[])
	{
		if (args.length != 2)
		{
			System.out.println("Usage: java Parser testFileToScan dataFileToRead");
			System.exit(-1);
		}

		scanner = new Scanner(args[0]);
		inputNumScanner = new Scanner(args[1]);
		// Continue the setup 
		varMap = new java.util.HashMap< String, Integer >();
		valueMap = new java.util.HashMap< String, Integer >();

		token = scanner.val;
		// computation();
		
	}
	
	
	// Use this function to accept a Token and and to get the next Token from the Scanner
	private boolean accept(String s) 
	{

		return true;
	}

	// Use this function whenever your program needs to expect a specific token
	private void expect(String s) 
	{
		if (accept(s)) 
			return;
		
		error();
		
	}

	private void error()
	{
		System.out.println("error");
	}
	
	// Implement this function to start parsing your input file
	public void computation() 
	{
		//inputNumScanner.Next();
		//inputNumber = inputNumScanner.val;

		token = scanner.sym;

		if (token != scanner.expressionMap.get("main"))
		{
			//its not main, output error.
			error();
			return;
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
	}

	public void varDec() 
	{
		scanner.Next();
		token = scanner.sym;
		while (token != 70)
		{
			if (token == 61)
			{
				varMap.put(scanner.Id2String(scanner.id), scanner.id);
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

	public void statSequence()
	{
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
				funcCall();
				//scanner.Next();
				continue;
			}
	
			if (scanner.sym == scanner.expressionMap.get("if"))
			{
				ifStatement();
				scanner.Next();
				continue;
			}

			if (scanner.sym == scanner.expressionMap.get("fi") || scanner.sym == scanner.expressionMap.get("else"))
			{
				return;
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
		
		if (scanner.sym == scanner.expressionMap.get("<-"))
		{
			scanner.Next();
			if (valueMap.containsKey(myIdent))
			{
				valueMap.replace(myIdent, exp());
			}
			else if (!valueMap.containsKey(myIdent) && varMap.containsKey(myIdent))
			{
				valueMap.put(myIdent, exp());
			}
			else // its not in the var map
			{
				error();
			}
		}
	}

	public int ifStatement()
	{
		int ret = 0;
		if (scanner.sym != scanner.expressionMap.get("else"))
			scanner.Next();
		int fiCount = 0;
		if (relation()) // If we pass the conditional check
		{
			//scanner.Next();
			if (scanner.sym == scanner.expressionMap.get("then"))
			{
				scanner.Next();
				statSequence();
			}
			if (scanner.sym == scanner.expressionMap.get("else"))
			{
				while (scanner.sym != scanner.expressionMap.get("fi"))
				{
					scanner.Next();
				}
				// go until we hit the next semicolon after the fi
				scanner.Next();
			}
			if (scanner.sym == scanner.expressionMap.get("fi"))
			{
				ret = 0;
				//scanner.Next();
			}
		}
		else
		{	// We go here if we don't pass the conditional check.
			// That means we have to either execute on the next "else", or if we see an invalid "if", pass it up by going to its "fi"
			while (scanner.sym != scanner.expressionMap.get("fi"))
			{
				scanner.Next();

				if (scanner.sym == scanner.expressionMap.get("else"))
				{
					// now we are at the else
					scanner.Next();
					statSequence();
				}
				if (scanner.sym == scanner.expressionMap.get("if"))
				{
					fiCount++;
					// go until it's a fi.
					while (fiCount > 0)
					{
						scanner.Next();
						if(scanner.sym == scanner.expressionMap.get("fi"))
						{
							fiCount--;
						}
						if(scanner.sym == scanner.expressionMap.get("if"))
						{
							fiCount++;
						}
					}
					scanner.Next();
				}
			}


			if (scanner.sym == 255)
				error();
		}
		return ret;
	}

	public boolean relation()
	{
		boolean ret;
		int exp1 = exp();
		int op = scanner.sym;
		scanner.Next();
		int exp2 = exp();
		if (op == scanner.expressionMap.get("=="))
		{
			ret = (exp1 == exp2);
		}
		else if (op == scanner.expressionMap.get("!="))
		{
			ret = (exp1 != exp2);
		}
		else if (op == scanner.expressionMap.get("<"))
		{
			ret = (exp1 < exp2);
		}
		else if (op == scanner.expressionMap.get("<="))
		{
			ret = (exp1 <= exp2);
		}
		else if (op == scanner.expressionMap.get(">"))
		{
			ret = (exp1 > exp2);
		}
		else if (op == scanner.expressionMap.get(">="))
		{
			ret = (exp1 >= exp2);
		}
		else
		{
			ret = false;
			error();
		}
		return ret;
	}

	int exp() {
		int ret; 
		int t = term();
		while (scanner.sym == scanner.expressionMap.get("+") || scanner.sym == scanner.expressionMap.get("-")) {
			boolean tempMinus = scanner.sym == scanner.expressionMap.get("-");
			scanner.Next();
			if (!tempMinus) // if we're substracting or adding.
			{
				t += term();
			}
			if (tempMinus)
			{
				t -= term();
			}
		}
		ret = t;
		
		return ret;
	}

	int term() {
		int ret;
		
		int t = factor();
		while (scanner.sym == scanner.expressionMap.get("*")) {
			scanner.Next();
			t *= factor();
		}
		while (scanner.sym == scanner.expressionMap.get("/")) {
			scanner.Next();
			t /= factor();
		}
		ret = t;
		return ret;
	}

	int factor() {
		int ret = 0;
		if (scanner.sym == 60) // is a number
		{
			ret = scanner.val;
			scanner.Next();
		}
		else if (scanner.sym == 61)
		{	// var identity
			if ( valueMap.get(scanner.Id2String(scanner.id)) != null)
				ret = valueMap.get(scanner.Id2String(scanner.id));
			else
				error();
			scanner.Next();
		}
		else if (scanner.sym == scanner.expressionMap.get("("))
		{	// expression
			scanner.Next();
			ret = exp();
			if (scanner.sym == scanner.expressionMap.get(")"))
			{
				scanner.Next();
			}
		}
		else if (scanner.sym == scanner.expressionMap.get("call"))
		{
			//scanner.Next();
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
			return inputNum();
		}
		if (myIdent.equals("outputnum"))
		{
			scanner.Next();
			if (scanner.sym == 61 && !varMap.containsKey(scanner.Id2String(scanner.id)))
				return 1;
			outputNum(exp());
		}
		if (myIdent.equals("outputnewline"))
		{
			outputnewline();
		}
		ret = 0;
		return ret;
	}

	int inputNum()
	{
		int val = inputNumScanner.val;
		inputNumScanner.Next();
		return val;
	}

	void outputNum(int a)
	{
		System.out.print(a);
	}

	void outputnewline()
	{
		System.out.print("\n");
	}

	public static void main(String[] args) 
	{
		Parser p = new Parser(args);
		p.computation();
	}
}