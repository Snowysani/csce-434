package edu.tamu.csce434;


public class Compiler 
{
	private edu.tamu.csce434.Scanner scanner;
	
	int buf[] = new int[3000];		
	
	
	// Constructor of your Compiler
	public Compiler(String args)
	{
		
		scanner = new Scanner(args);
		
	}
	
	
	
	// Implement this function to start compiling your input file
	public int[] getProgram()  
	{
		expect("main");
		if (peek("var")) 
			declarations();
		expect("{");
		statementSequence();
		expect("}");
		expect(".");
		
		scanner.closefile();
		
		return buf;
	}


}