package edu.tamu.csce434;

import java.util.Objects;
import java.util.Vector;
import java.util.Map.Entry;



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
		
		printError();
		
	}

	private void error()
	{
		System.out.println("error");
	}
	
	// Implement this function to start parsing your input file
	public void computation() 
	{
		inputNumScanner.Next();
		inputNumber = inputNumScanner.val;

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
			else
			{
				token = 0;
				error();
			}
		}

		// at this point we should have a semicolon
		if (token == scanner.expressionMap.get(";"))
		{
			scanner.Next();
			token = scanner.sym;
		}
		else
		{
			error();
			scanner.Next();
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
		while (scanner.sym != scanner.expressionMap.get("."))
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
				scanner.Next();
				continue;
			}
	
			if (scanner.sym == scanner.expressionMap.get("if"))
			{
				ifStatement();
				continue;
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
				valueMap.replace(myIdent, exp());
			else
				valueMap.put(myIdent, exp());
		}
	}

	public int ifStatement()
	{
		int ret = 0;
		if (relation())
		{
			scanner.Next();
			if (scanner.sym == scanner.expressionMap.get("then"))
			{
				statSequence();
			}
		}
		else
		{
			scanner.Next();
			while (scanner.sym != scanner.expressionMap.get("fi"))
			{
				scanner.Next();
				if (scanner.sym == scanner.expressionMap.get("else"))
				{
					statSequence();
				}
			}
		}
		return ret;
	}

	public boolean relation()
	{
		boolean ret;
		int exp1 = exp();
		scanner.Next();
		int op = scanner.val;
		scanner.Next();
		int exp2 = exp();
		scanner.Next();
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
		while (scanner.sym == scanner.expressionMap.get("+")) {
			scanner.Next();
			t += term();
		}
		while (scanner.sym == scanner.expressionMap.get("-")) {
			scanner.Next();
			t += term();
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
			scanner.Next();
		}
		else if (scanner.sym == scanner.expressionMap.get("("))
		{	// expression
			scanner.Next();
			ret = exp();
		}
		else if (scanner.sym == scanner.expressionMap.get("call"))
		{
			scanner.Next();
			ret = funcCall();
		}
		else
		{
			ret = 0; error();
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
		inputNumScanner.Next();
		return inputNumScanner.val;
	}

	void outputNum(int a)
	{
		System.out.println(a);
	}

	void outputnewline()
	{
		System.out.println("\n");
	}

	public static void main(String[] args) 
	{
		Parser p = new Parser(args);
		p.computation();
	}
}