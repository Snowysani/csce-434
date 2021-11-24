package edu.tamu.csce434;

import java.io.FileReader;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.Map.Entry;


public class Scanner {
    public int sym; // current token on the input 
    public int val; // value of last number encountered
    public int id;  // index of last identifier encountered 

    public FileReader fileReader;
    public BufferedReader bufferedReader;

    public String contents;
    public int contentsPointer;


    java.util.Map< Integer, String > symMap =
            new java.util.HashMap< Integer, String >();

    java.util.Map< String, Integer > expressionMap =
            new java.util.HashMap< String, Integer >();

    java.util.Map< Integer, String > idMap =
            new java.util.HashMap< Integer, String >();

	public void closefile()
	{
        try{
            fileReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
	}

    public static boolean isNumeric(String str) { 
        try {  
          Double.parseDouble(str);  
          return true;
        } catch(NumberFormatException e){  
          return false;  
        }  
      }

    public static boolean isLetters(String s) {
        if (s == null){ // checks if the String is letters 
           return false;
        }
        int len = s.length();
        for (int i = 0; i < len; i++) {
           // checks whether the character is not a letter
           // if it is not a letter ,it will return false
           if ((Character.isAlphabetic(s.charAt(i)) == false)) {
              return false;
           }
        }
        return true;
     }

    public boolean idFlag;
    public void grabNextIdentifier()
    {
        this.sym = 61;
        this.id = contentsPointer;
        String tempId = "";
        while (contents.charAt(contentsPointer) != '\n'
            && contents.charAt(contentsPointer) != '\t'
            && contents.charAt(contentsPointer) != ','
            && contents.charAt(contentsPointer) != ';'
            && contents.charAt(contentsPointer) != '<'
            && contents.charAt(contentsPointer) != '('
            && contents.charAt(contentsPointer) != '+'
            && contents.charAt(contentsPointer) != '-'
            && contents.charAt(contentsPointer) != '/'
            && contents.charAt(contentsPointer) != '*'
            && contents.charAt(contentsPointer) != ')'
            && contents.charAt(contentsPointer) != '['
            && contents.charAt(contentsPointer) != ']')
        {
            if (contents.charAt(contentsPointer) != ' ' )
                tempId += contents.charAt(contentsPointer);
            if (contents.charAt(contentsPointer) == ' ')
            {
                contentsPointer++;
                break;
            }
            contentsPointer++;
        }

        if (tempId.isEmpty())
        {
            tempId += contents.charAt(contentsPointer);
            if (expressionMap.get(tempId) != null)
            {
                if (expressionMap.get(tempId + contents.charAt(contentsPointer + 1)) != null )
                {
                    this.sym = expressionMap.get(tempId + contents.charAt(contentsPointer + 1));
                    contentsPointer += 2; // usually ++, but increment twice here.
                }
                else
                {
                    this.sym = expressionMap.get(tempId);
                    contentsPointer++;
                }
            }
            else
            {
                grabNextIdentifier();
            }
        }
        else if (isNumeric(tempId))
        {
            while (contentsPointer < contents.length())
            {
                if (!isNumeric(tempId + contents.charAt(contentsPointer)))
                {
                    break; // if we are about to encounter a non-numerical value, get out of this loop.
                }
                tempId += contents.charAt(contentsPointer);
                contentsPointer++;
            }
            this.sym = 60;
            this.val = Integer.parseInt(tempId);
        }
        else
        {
            if (expressionMap.get(tempId) != null)
            {
                if (expressionMap.get(tempId + contents.charAt(contentsPointer)) != null)
                {
                    this.sym = expressionMap.get(tempId + contents.charAt(contentsPointer));
                    contentsPointer++;
                }
                else
                {
                    this.sym = expressionMap.get(tempId);
                }
            }
            else if (!tempId.isEmpty())
            {
                idMap.put(this.id, tempId);
            }
            else
            {
                grabNextIdentifier();
            }
        }

        // Do we keep going on the next one?
        if (contents.charAt(contentsPointer) == '\n' || contents.charAt(contentsPointer) == ';')
        {
            idFlag = false;
        }
    }
	/** 
	 * Advance to the next token 
	 */
    public void Next() {
        try {
            if (idFlag)
                grabNextIdentifier();
            else
            {
                int originalPointer = contentsPointer;
                String tempToken = "";
                while (contents.length() > contentsPointer)
                {
                    idFlag = false;
                    if (contents.charAt(contentsPointer) != '\n' 
                        && contents.charAt(contentsPointer) != '\t' 
                        && contents.charAt(contentsPointer) != ' ')
                    {
                        tempToken += contents.charAt(contentsPointer);
                    }
                    if(expressionMap.get(tempToken) != null)
                    {   
                        this.sym = expressionMap.get(tempToken);
                        if (this.sym == 2 && contents.charAt(contentsPointer + 1) == '/')
                        {
                            // Then it's a comment.
                            // Means we go until we hit a new line.
                            contentsPointer++;
                            while (contents.charAt(contentsPointer) != '\n')
                            {
                                tempToken += contents.charAt(contentsPointer);
                                contentsPointer++;
                            }
                            contentsPointer++;
                            Next();
                            break;
                        }
                        if (this.sym == 22 && contents.charAt(contentsPointer + 1) == '-')
                        {
                            // then it's an edge case: becomes
                            this.sym = 40;
                            contentsPointer++;
                        }
                        if (this.sym == 22 && contents.charAt(contentsPointer + 1) == '=')
                        {
                            // then it's an edge case: leq
                            this.sym = 24;
                            contentsPointer++;
                        }
                        if (this.sym == 25 && contents.charAt(contentsPointer + 1) == '=')
                        {
                            // then it's an edge case: geq
                            this.sym = 23;
                            contentsPointer++;
                        }
                        if (this.sym == 110 || this.sym == 111)
                        {
                            // It's a var. We need to treat the rest of the line as identifiers.
                            idFlag = true;
                            contentsPointer++;
                            // Until we see a semicolon or an end of line, every next thing is a identifier.
                            // Can have multiple identifiers, separated by comma. 
                            break;
                        }
                        if (this.sym == 100)
                        {
                            // it's a call. we need to get the next identifier.
                            idFlag = true;
                            contentsPointer++;
                            break;
                        }
                        contentsPointer++;
                        break;
                    }
                    if (idMap.containsValue(tempToken) && !isLetters(tempToken + contents.charAt(contentsPointer + 1)))
                    {
                        // then we know it's a var id.
                        this.sym = 61;
                        for (Entry<Integer, String> entry : idMap.entrySet()) {
                            if (Objects.equals(entry.getValue(), tempToken)) {
                                this.id = entry.getKey();
                            }
                        }
                        contentsPointer++;
                        break;
                    }
                    if (isNumeric(tempToken))
                    {
                        contentsPointer++;
                        // We know it's a number. keep going until it's not a number.
                        while (isNumeric(contents.substring(contentsPointer, contentsPointer + 1)))
                        {
                            tempToken += contents.charAt(contentsPointer);
                            contentsPointer++;
                        }
                        this.val = Integer.parseInt(tempToken);
                        this.sym = 60;
                        break;
                    }
                    if ( 
                            (!tempToken.isEmpty()
                                && (contentsPointer + 1) < contents.length() &&
                                (contents.charAt(contentsPointer + 1) == ' '
                                || contents.charAt(contentsPointer + 1) == '}'
                                || contents.charAt(contentsPointer + 1) == ')'
                                || contents.charAt(contentsPointer + 1) == '\n'
                                || contents.charAt(contentsPointer + 1) == ','
                                || contents.charAt(contentsPointer + 1) == '('
                                || contents.charAt(contentsPointer + 1) == '['
                                || contents.charAt(contentsPointer + 1) == ']')
                            ) 
                            ||
                            (!tempToken.isEmpty() && (contentsPointer + 1 >= contents.length()))
                        )
                    {
                        if (!isLetters(tempToken))
                        {
                            this.sym = 0;
                            contentsPointer++;
                            break;
                        }
                        if (tempToken.equals("call"))
                        {
                            this.sym = 100;
                            contentsPointer++;
                            break;
                        }
                        else if (expressionMap.get(tempToken) != null)
                        {
                            if (expressionMap.get(tempToken + contents.charAt(contentsPointer+1)) != null)
                            {
                                this.sym = expressionMap.get(tempToken + contents.charAt(contentsPointer+1));
                                contentsPointer++;
                            }
                            else
                            {
                                this.sym = expressionMap.get(tempToken);
                            }
                            break;
                        }
                        else
                        {
                            this.sym = 61;
                            idMap.put(this.id, tempToken);
                            contentsPointer++;
                            break;
                        }
                    }
                    contentsPointer++;
                }
                if (contentsPointer >= contents.length() && tempToken.isEmpty())
                {
                    this.sym = 255;
                }
                if (contentsPointer >= contents.length() && !tempToken.isEmpty())
                {

                    this.sym = expressionMap.get(tempToken);
                    
                }
            }

        } catch (Exception e){
            this.sym = 255;
            e.printStackTrace();
        }
	}
    /**
     * Move to next char in the input
     */
	public void Advance() {
        contentsPointer++;
	}

    public Scanner (String fileName) {     
        try 
        {
            fileReader = new FileReader(fileName);
            bufferedReader = new BufferedReader(fileReader);
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
            fileReader.close();  
            contents = stringBuilder.toString();

            idFlag = false;
            populateSymMap(symMap); populateExMap(expressionMap);
            Next();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts given id to name; returns null in case of error
     */
    public String Id2String(int id) { 
        if (idMap.get(id) != null)
            return idMap.get(id);
        else
            return null;
    }

    /**
     * Signal an error message
     * 
     */
    public void Error(String errorMsg) {
        System.out.println(errorMsg);
    }

    /**
     * Converts given name to id; returns -1 in case of error
     */
    public int String2Id(String name) {
        return -1;   
    }

    public void populateSymMap(java.util.Map< Integer, String > symMap)
    {
        symMap.put(  0, "error");
        symMap.put(  1, "times");
        symMap.put(  2, "div");
        symMap.put( 11, "plus");
        symMap.put( 12, "minus");
        symMap.put( 20, "eql");
        symMap.put( 21, "neq");
        symMap.put( 22, "lss");
        symMap.put( 23, "geq");
        symMap.put( 24, "leq");
        symMap.put( 25, "gtr");
        symMap.put( 30, "period");
        symMap.put( 31, "comma");
        symMap.put( 32, "openbracket");
        symMap.put( 34, "closebracket");
        symMap.put( 35, "closeparen");
        symMap.put( 40, "becomes");
        symMap.put( 41, "then");
        symMap.put( 42, "do");
        symMap.put( 50, "openparen");
        symMap.put( 60, "number");
        symMap.put( 61, "ident");
        symMap.put( 70, "semicolon");
        symMap.put( 77, "let");
        symMap.put( 80, "end");
        symMap.put( 81, "od");
        symMap.put( 82, "fi");
        symMap.put( 90, "else");
        symMap.put(100, "call");
        symMap.put(101, "if");
        symMap.put(102, "while");
        symMap.put(103, "return");
        symMap.put(110, "var");
        symMap.put(111, "arr");
        symMap.put(112, "function");
        symMap.put(113, "procedure");
        symMap.put(150, "begin");
        symMap.put(200, "main");
        symMap.put(222, "comment");
        symMap.put(255, "eof");
    }
    public void populateExMap(java.util.Map< String, Integer > exMap)
    {
        exMap.put("err", 0);
        exMap.put("*", 1);
        exMap.put("/", 2);
        exMap.put("+", 11);
        exMap.put("-", 12);
        exMap.put("==", 20);
        exMap.put("!=", 21);
        exMap.put("<", 22);
        exMap.put(">=", 23);
        exMap.put("<=", 24);
        exMap.put(">", 25);
        exMap.put(".", 30);
        exMap.put(",", 31);
        exMap.put("[", 32);
        exMap.put("]", 34);
        exMap.put(")", 35);
        exMap.put("<-", 40);
        exMap.put("then", 41);
        exMap.put("do", 42);
        exMap.put("return", 103);
        exMap.put("(", 50);
        exMap.put(";", 70);
        exMap.put("let", 77);
        exMap.put("}", 80);
        exMap.put("od", 81);
        exMap.put("fi", 82);
        exMap.put("else", 90);
        exMap.put("call", 100);
        exMap.put("if", 101);
        exMap.put("while", 102);
        exMap.put("return", 103);
        exMap.put("var", 110);
        exMap.put("array", 111);
        exMap.put("function", 112);
        exMap.put("procedure", 113);
        exMap.put("{", 150);
        exMap.put("main", 200);
        exMap.put("//", 222);
    }
}
