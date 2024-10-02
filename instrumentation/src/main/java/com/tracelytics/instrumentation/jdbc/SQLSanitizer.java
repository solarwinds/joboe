package com.tracelytics.instrumentation.jdbc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.logging.Logger.Level;

/**
 * Sanitizer that removes literals from SQL statement. The behaviors of the sanitization rely on the Driver class and the sanitize flag.
 * 
 * Take note that this class deploys a State machine, that mostly referenced code from the existing C implementation for php oboe
 * 
 * @author Patson Luk
 * @see https://github.com/tracelytics/oboe/blob/master/contrib/php_oboe/sql_parser.c
 *
 */
public class SQLSanitizer {
    private static Logger logger = LoggerFactory.getLogger();
    private static final char REPLACEMENT_CHAR = '?'; //used to replace the literals
    
    private final Map<Character, Character> LITERAL_QUOTES = new HashMap<Character, Character>(); //characters that are used to quote String literals, key: opening quote, value: closing quote
    private final Map<Character, Character> IDENTIFIER_QUOTES  = new HashMap<Character, Character>(); //characters that are used to quote identifiers, key: opening quote, value: closing quote
    
    private static final char ESCAPE = '\\'; //SQL escape character
    
    public static final int DISABLED = 0; //Do not sanitize
    public static final int ENABLED_AUTO = 1;  //Sanitize solely based on the driver class
    public static final int ENABLED_DROP_DOUBLE_QUOTED = 2; //Sanitize based on this flag (drop double quotes, ie. treat double quoted values as String literals), the rest of the behavior based on the driver class 
    public static final int ENABLED_KEEP_DOUBLE_QUOTED = 4; //Sanitize based on this flag (keep double quotes, ie. treat double quoted values as Identifiers), the rest of the behavior based on the driver class
    
    private static Set<Character> SQL_OPERATOR_CHARS = new HashSet<Character>(); //
    static {
        SQL_OPERATOR_CHARS.add('.');
        SQL_OPERATOR_CHARS.add(';');
        SQL_OPERATOR_CHARS.add('(');
        SQL_OPERATOR_CHARS.add(')');
        SQL_OPERATOR_CHARS.add(',');
        SQL_OPERATOR_CHARS.add('+');
        SQL_OPERATOR_CHARS.add('-');
        SQL_OPERATOR_CHARS.add('/');
        SQL_OPERATOR_CHARS.add('*');
        SQL_OPERATOR_CHARS.add('|');
        SQL_OPERATOR_CHARS.add('=');
        SQL_OPERATOR_CHARS.add('!');
        SQL_OPERATOR_CHARS.add('^');
        SQL_OPERATOR_CHARS.add('>');
        SQL_OPERATOR_CHARS.add('<');
        SQL_OPERATOR_CHARS.add('[');
        SQL_OPERATOR_CHARS.add(']');
    }
    
    private SQLSanitizer() {
        //force usage of getSanitizer()
    }
    
    /**
     * Obtains a sanitizer based on the sanitize flag and statement class name (driver)
     * @param sanitizeFlag
     * @param statementClassName
     * @return Sanitizer based on the flag and driver class. null if the sanitize flag is set to DISABLED
     */
    public static final SQLSanitizer getSanitizer(int sanitizeFlag, String statementClassName) {
        if (sanitizeFlag == DISABLED) {
            return null;
        }
        
        SQLSanitizer sanitizer = new SQLSanitizer();
        
        sanitizer.LITERAL_QUOTES.put('\'', '\''); //single quote is always a String literal quote
      
        
        if (sanitizeFlag == ENABLED_DROP_DOUBLE_QUOTED) { 
            sanitizer.LITERAL_QUOTES.put('"', '"'); //then consider double quote as String literal quote (so it will be dropped)
        } else if (sanitizeFlag == ENABLED_KEEP_DOUBLE_QUOTED) { 
            sanitizer.IDENTIFIER_QUOTES.put('"', '"'); //then consider double quote as Identifier quote
        } else { //auto
            if (statementClassName.contains("postgresql") || statementClassName.contains("oracle")) { //PostgreSQL and Oracle SQL use double quotes for identifiers
                sanitizer.IDENTIFIER_QUOTES.put('"', '"'); 
            } else { //any other DB servers, we assume double quotes mean literals
                sanitizer.LITERAL_QUOTES.put('"', '"');
            }
        }
        
        // PostgreSQL's double-dollar quoted literal: $optionalTag$literal$optionalTag$
        if (statementClassName.contains("postgresql")) {
            sanitizer.LITERAL_QUOTES.put('$', '$');
        }
        
        //extra characters for quoted identifiers (refer to http://wiki.ispirer.org/sqlways/db2/identifiers)
        if (statementClassName.contains("mysql")) {
            sanitizer.IDENTIFIER_QUOTES.put('`', '`'); //back quote is Identifier quote for MySQL
        } else if (statementClassName.contains("sybase") || statementClassName.contains("microsoft")) {
            sanitizer.IDENTIFIER_QUOTES.put('[', ']'); //[] is Identifier quote for Sybase and MS SQL
        }
        
        return sanitizer;
    }
    
    /**
     * Sanitizes the source query. The maxOutputLength is used to avoid excessive processing
     * @param query             The source SQL query
     * @return the sanitized string
     */
    public final String sanitizeQuery(String query) {
        StringBuilder output = new StringBuilder();
        // For PostgreSQL's double-dollar quoted literal only
        StringBuilder tagBuilder = new StringBuilder().append('$');
        String tag = "";
        
        State currentState = State.FSM_COPY;
        State previousState = null;
        
        int counter = 0;
        Character closingQuoteChar = null; //the closing quote character to search for
        
        
        for (int i = 0 ; i < query.length(); i ++) {  
            char currentChar = query.charAt(i);
            if (currentState != previousState && logger.shouldLog(Level.TRACE)) {
                logger.trace(String.format("oboe_sanitize_sql: New state=%s(%d) on char@%d='%c'\n", currentState.toString(), currentState.ordinal(), counter, currentChar));
                previousState = currentState;
            }

            counter ++;

            switch (currentState) {

            case FSM_STRING_START:
                // Handle PostgreSQL's double-dollar quoted literal: $optionalTag$literal$optionalTag$
                if (closingQuoteChar == '$') {
                    // The real end of the opening quote
                    if (currentChar == '$') {
                        currentState = State.FSM_STRING_BODY;
                        output.append(REPLACEMENT_CHAR);
                        tag = tagBuilder.append('$').toString();
                        tagBuilder.delete(1, tagBuilder.length()); // Keep the first '$'
                    } else { // still the tag
                        tagBuilder.append(currentChar);
                    }
                    break;
                }
    
                /* Add deleted-text marker to indicate that we've sanitized it
                 */
                output.append(REPLACEMENT_CHAR);
                
                /* Handle any special string opening conditions. */
                if (currentChar == closingQuoteChar) {
                    /* We have an empty string - exit string state. */
                    currentState = State.FSM_STRING_END;
                } else if (currentChar == ESCAPE) {
                    currentState = State.FSM_STRING_ESCAPE;
                } else {
                    currentState = State.FSM_STRING_BODY;
                }
                
                break;

            case FSM_STRING_BODY:
                if (currentChar == closingQuoteChar) {
                    if ((closingQuoteChar == '$') && (!query.regionMatches(i, tag, 0, tag.length()))){
                        /* Do nothing - not the string end. It happens when there is dollar signs in the
                         literal, e.g., $tag$cost=$9..$tag$ */
                    } else {
                        currentState = State.FSM_STRING_END;
                    }
                } else if ((currentChar == ESCAPE) && (closingQuoteChar != '$')){ 
                    //Make sure the string body is not the string content of Postgres' Dollar-quoted String Constant, 
                    //as for such a literal, nothing should escape within the string content according to https://www.postgresql.org/docs/9.3/sql-syntax-lexical.html
                    currentState = State.FSM_STRING_ESCAPE;
                } else {
                    /* Do nothing - we're dropping the character. */
                }
                break;

            case FSM_STRING_ESCAPE:
                /* Whatever the current character is, drop it. */
                currentState = State.FSM_STRING_BODY;
                break;

            case FSM_STRING_END:
                // Handle PostgreSQL's double-dollar quoted string
                if (closingQuoteChar == '$') {
                    if (currentChar == '$') {
                        // The real end of the closing quote
                        currentState = State.FSM_COPY;
                    } else {
                        // still the tag, do nothing
                    }
                    break;
                }
                
                /* Check if we've reached the end of the string. */
                if (currentChar == closingQuoteChar) {
                    /* We got a twinned single quote so it's still part of the body*/
                    currentState = State.FSM_STRING_BODY;
                } else {
                    output.append(currentChar);
                    currentState = State.FSM_COPY;
                }
                break;

            case FSM_COPY_ESCAPE:
                /* Whatever the current character is, copy it. */
                output.append(currentChar);
                currentState = State.FSM_COPY;
                break;

            case FSM_NUMBER:
                if (Character.isDigit(currentChar)) { //part of the number
//                    output.append(currentChar);
//                    currentState = State.FSM_COPY;
                } else if (currentChar == '.' || currentChar == 'E'){ //decimal / mantissa E exponent
                    currentState = State.FSM_NUMBERIC_EXTENSION;
                } else { //end of the number
                    output.append(currentChar);
                    currentState = State.FSM_COPY;   
                }
                break;
                
            case FSM_NUMBERIC_EXTENSION:
                currentState = State.FSM_NUMBER; //just consume whatever follows the E/'.' and proceed with number
                break;

            case FSM_IDENTIFIER:
                if (LITERAL_QUOTES.containsKey(currentChar)) { //String literals can be like X'80'
                    Character top1 = Character.toUpperCase(output.charAt(output.length()-1));
                    Character top2 = Character.toUpperCase(output.charAt(output.length()-2));
                    if ((top1 == 'X') || (top1 == 'B') || (top1 == 'U') || (top1 == 'N')) {
                        output.deleteCharAt(output.length()-1);
                    } else if ((top1 == '&') && (top2 == 'U')) {
                        output.delete(output.length()-2, output.length());
                    }
                    /* Start of a string - identifier is probably a string encoding prefix. */
                    closingQuoteChar = LITERAL_QUOTES.get(currentChar);
                    currentState = State.FSM_STRING_START;
                } else {
                    output.append(currentChar);
                    if (Character.isWhitespace(currentChar) || SQL_OPERATOR_CHARS.contains(currentChar)) {
                        /* Not part of the identifier. */
                        currentState = State.FSM_COPY;
                    }
                }
                break;
                
            case FSM_QUOTED_IDENTIFIER:
                output.append(currentChar);
                if (currentChar == closingQuoteChar) {
                    currentState = State.FSM_COPY; //end of the quoted identifier
                } else if (currentChar == ESCAPE) {
                    currentState = State.FSM_QUOTED_IDENTIFIER_ESCAPE;
                }
                break;
                
            case FSM_QUOTED_IDENTIFIER_ESCAPE:
                /* Whatever the current character is, copy it. */
                output.append(currentChar);
                currentState = State.FSM_QUOTED_IDENTIFIER;
                break;
                
            case FSM_COPY:
            default:
                
                /* Handle special character. */
                if (LITERAL_QUOTES.containsKey(currentChar)) {
                    /* Start of a single-quoted string. */
                    closingQuoteChar = LITERAL_QUOTES.get(currentChar);
                    currentState = State.FSM_STRING_START;
                } else if (IDENTIFIER_QUOTES.containsKey(currentChar)) {
                    /* Start of a quoted identifier. */
                    output.append(currentChar);
                    closingQuoteChar = IDENTIFIER_QUOTES.get(currentChar);
                    currentState = State.FSM_QUOTED_IDENTIFIER;
                } else if (currentChar == ESCAPE) {
                    output.append(currentChar);
                    currentState = State.FSM_COPY_ESCAPE;
                } else if (Character.isLetter(currentChar) || currentChar == '_') {
                    /* Start of an unquoted identifier. */
                    output.append(currentChar);
                    currentState = State.FSM_IDENTIFIER;
                } else if (Character.isDigit(currentChar)) {
                    /* Start of a numeric literal. */
                    output.append(REPLACEMENT_CHAR);
                    currentState = State.FSM_NUMBER;
                } else {
                    output.append(currentChar);
                }
                break;
            }
        }
        
        return output.toString();
    }
    
    static enum SanitizeMode {
        DROP_DOUBLE_QUOTES, KEEP_DOUBLE_QUOTES
    }
    
    private enum State {
        FSM_COPY("copy"), /*!< Copying input directly - default state. */
        FSM_COPY_ESCAPE("copy escape"), /*!< Copying an escaped character code. */
        FSM_STRING_START("string start"), /*!< Parsing an opening quote for a possible single-quoted string. */
        FSM_STRING_BODY("string body"), /*!< Parsing a single-quoted string. */
        FSM_STRING_ESCAPE("string escape"), /*!< Parsing an escape code in a single-quoted string. */
        FSM_STRING_END("string end"), /*!< Parsing a possible closing single quote. */
        FSM_NUMBER("number"), /*!< Parsing a numeric literal. */
        FSM_NUMBERIC_EXTENSION("decimal point or exponent"), 
        FSM_IDENTIFIER("identifier"), /*!< Parsing an unquoted identifier. */
        FSM_QUOTED_IDENTIFIER("quoted identifier"), 
        FSM_QUOTED_IDENTIFIER_ESCAPE("quoted identifier escape"); /*!< Parsing an escape code in quoted identifier. */
        
        private String description;
        
        private State(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    
   
}
