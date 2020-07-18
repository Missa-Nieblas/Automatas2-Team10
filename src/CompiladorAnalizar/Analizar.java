package CompiladorAnalizar;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class Analizar 
{
	int renglon=1;
	ArrayList<String> impresion; 
	ArrayList<Identificador> identi = new ArrayList<Identificador>();
	ListaDoble<Token> tokens;
	ArrayList<String> aux = new ArrayList<String>();
	final Token vacio=new Token("", 9,0);
	boolean bandera=true;

	public ArrayList<Identificador> getIdenti() {
		return identi;
	}
	public Analizar(String ruta) {//Recibe el nombre del archivo de texto
		analisaCodigo(ruta);
		if(bandera) {
			impresion.add("No hay errores lexicos");
			analisisSintactico(tokens.getInicio());
		}
		if(impresion.get(impresion.size()-1).equals("No hay errores lexicos"))
			impresion.add("No hay errores sintacticos");
			analisisSemantico(tokens.getInicio());
	}
	//Entrada de Codigo y analiza
	public void analisaCodigo(String ruta) {
		String linea="", token="";
		StringTokenizer tokenizer;
		try{
	          FileReader file = new FileReader(ruta);
	          BufferedReader archivoEntrada = new BufferedReader(file);
	          linea = archivoEntrada.readLine();
	          impresion=new ArrayList<String>();
	          tokens = new ListaDoble<Token>();
	          while (linea != null){
	        	    linea = separaDelimitadores(linea);
	                tokenizer = new StringTokenizer(linea);
	                while(tokenizer.hasMoreTokens()) {
	                	token = tokenizer.nextToken();
	                	analisisLexico(token);
	                }
	                linea=archivoEntrada.readLine();
	                renglon++;
	          }
	          archivoEntrada.close();
		}catch(IOException e) {
			JOptionPane.showMessageDialog(null,"No se encontro el archivo favor de checar la ruta","Alerta Chavo",JOptionPane.ERROR_MESSAGE);
		}
	}
	//AnalisisSintactico
	public Token analisisSintactico(NodoDoble<Token> nodo) {
		Token  to;
		if(nodo!=null) // por si acaso :b
		{
			to =  nodo.dato;
			
			switch (to.getTipo())
			{
			case Token.MODIFICADOR:
				int sig=nodo.siguiente.dato.getTipo();
				 
				if(sig!=Token.TIPO_DATO && sig!=Token.CLASE)
					impresion.add("Error sinatactico en la linea "+to.getLinea()+" se esparaba un tipo de dato");
				break;
			case Token.IDENTIFICADOR:
				// lo que puede seguir despues de un idetificador
				if(!(Arrays.asList("{","=",";").contains(nodo.siguiente.dato.getValor()))) 
					impresion.add("Error sinatactico en la linea "+to.getLinea()+" se esparaba un simbolo");
				else
					if(nodo.anterior.dato.getValor().equals("class")) 
					{
						identi.add( new Identificador(to.getValor(), " ", "class", "Global", nodo.dato.getLinea()));
					}
				break;
		
			case Token.TIPO_DATO:
			case Token.CLASE:
				// si lo anterior fue modificador
				if (nodo.anterior!=null)
					if(cuenta(nodo.siguiente.dato.getValor())>1) {
						
					}else {
						if(nodo.anterior.dato.getTipo()==Token.MODIFICADOR) {
							if(nodo.siguiente.dato.getTipo()!=Token.IDENTIFICADOR) 
								impresion.add("Error sinatactico en la linea "+to.getLinea()+" se esparaba un identificador");
					}else
						impresion.add("Error sinatactico en la linea "+to.getLinea()+" se esperaba un modificador");
					}
					break;
			case Token.SIMBOLO:
				// Verificar que el mismo numero de parentesis y llaves que abren sean lo mismo que los que cierran
				if(to.getValor().equals("}")) 
				{
					if(cuenta("{")!=cuenta("}"))
						impresion.add("Error sinatactico en la linea "+to.getLinea()+ " falta un {");
				}else if(to.getValor().equals("{")) {
					if(cuenta("{")!=cuenta("}"))
						impresion.add("Error sinatactico en la linea "+to.getLinea()+ " falta un }");
				}
				else if(to.getValor().equals("(")) {
					if(cuenta("(")!=cuenta(")"))
						impresion.add("Error sinatactico en la linea "+to.getLinea()+ " falta un )");
					else
					{
						if(!(nodo.anterior.dato.getValor().equals("if")&&nodo.siguiente.dato.getTipo()==Token.CONSTANTE)) {
							impresion.add("Error sinatactico en la linea "+to.getLinea()+ " se esperaba un valor");
						}
					}
				}else if(to.getValor().equals(")")) {
					if(cuenta("(")!=cuenta(")"))
						impresion.add("Error sinatactico en la linea "+to.getLinea()+ " falta un (");
				}
				// verificar la asignacion
				else if(to.getValor().equals("=")){
					if(nodo.anterior.dato.getTipo()==Token.IDENTIFICADOR) {
						if(nodo.siguiente.dato.getTipo()!=Token.CONSTANTE)
							impresion.add("Error sinatactico en la linea "+to.getLinea()+ " se esperaba una constante");
						else {
							if(nodo.anterior.anterior.dato.getTipo()==Token.TIPO_DATO)
								identi.add(new Identificador(nodo.anterior.dato.getValor(),nodo.siguiente.dato.getValor(),nodo.anterior.anterior.dato.getValor(),"Global",nodo.dato.getLinea()));
						}
					}else
							//Por si se llega a repetir
							if(cuenta(nodo.anterior.dato.getValor())>=2) {
								
							}else {
								//Por si se pierde una variable declarada
								if(cuenta(nodo.anterior.dato.getValor())<2) {
									impresion.add("Error sinatactico en linea "+to.getLinea()+ " variable no declarada");
								}else {
									impresion.add("Error sinatactico en linea "+to.getLinea()+ " se esperaba un tipo de dato");
								}
							}
					}else 
				impresion.add("Error sinatactico en linea "+to.getLinea()+ " se esperaba un identificador");
			
				break;

			case Token.CONSTANTE:
				if(nodo.anterior.dato.getValor().equals("="))
					if(nodo.siguiente.dato.getTipo()!=Token.OPERADOR_ARITMETICO&&nodo.siguiente.dato.getTipo()!=Token.CONSTANTE&&!nodo.siguiente.dato.getValor().equals(";"))
						impresion.add("Error sinatactico en linea "+to.getLinea()+ " asignacion no valida");
				break;
			case Token.PALABRA_RESERVADA:
				// verificar estructura de if
				if(to.getValor().equals("if"))
				{
					if(!nodo.siguiente.dato.getValor().equals("(")) {
						impresion.add("Error sintactico en linea "+to.getLinea()+ " se esperaba un (");
					}
				}
				else 
				{
					// si es un else, buscar en los anteriores y si no hay un if ocurrira un error
					NodoDoble<Token> aux = nodo.anterior;
					boolean bandera=false;
					while(aux!=null&&!bandera) {
						if(aux.dato.getValor().equals("if"))
							bandera=true;
						aux =aux.anterior;
					}
					if(!bandera)
						impresion.add("Error sinatactico en linea "+to.getLinea()+ " else no valido");
				}
				break;
			case Token.OPERADOR_LOGICO:
				// verificar que sea  'numero' + 'operador' + 'numero' 
				if(nodo.anterior.dato.getTipo()!=Token.CONSTANTE) 
					impresion.add("Error sinatactico en linea "+to.getLinea()+ " se esperaba una constante");
				if(nodo.siguiente.dato.getTipo()!=Token.CONSTANTE)
					impresion.add("Error sinatactico en linea "+to.getLinea()+ " se esperaba una constante");
				break;
				
			case Token.OPERADOR_ARITMETICO:
				if(nodo.anterior.dato.getTipo()!=Token.CONSTANTE) 
					impresion.add("Error sinatactico en linea "+to.getLinea()+ " se esperaba una constante");
				if(nodo.siguiente.dato.getTipo()!=Token.CONSTANTE)
					impresion.add("Error sinatactico en linea "+to.getLinea()+ " se esperaba una constante");

				String aux1="", aux2="";
				aux1 = TipoDato(nodo.anterior.dato.getValor());
				aux2 = TipoDato(nodo.siguiente.dato.getValor());
				if(!aux1.equals(aux2))
					impresion.add("No se puede pude realizar la operacion en la linea "+to.getLinea());
				break;
			}
			analisisSintactico(nodo.siguiente); // buscar el siguiente de forma recursiva

			return to;
		}
		return  vacio;// para no regresar null y evitar null pointer
	}
	//Analisis Lexico :b
	public void analisisLexico(String token) {
		int tipo=0;
		//Se usan listas con los tipos de token
		
		if(Arrays.asList("public","static","private").contains(token)) 
			tipo = Token.MODIFICADOR;
		else if(Arrays.asList("if","else").contains(token)) 
			tipo = Token.PALABRA_RESERVADA;
		else if(Arrays.asList("int","char","float","boolean").contains(token))
			tipo = Token.TIPO_DATO;
		else if(Arrays.asList("(",")","{","}","=",";").contains(token))
			tipo = Token.SIMBOLO;
		else if(Arrays.asList("<","<=",">",">=","==","!=").contains(token))
			tipo = Token.OPERADOR_LOGICO;
		else if(Arrays.asList("+","-","*","/").contains(token))
			tipo = Token.OPERADOR_ARITMETICO;
		else if(Arrays.asList("true","false").contains(token)||Pattern.matches("^\\d+$",token)) 
			tipo = Token.CONSTANTE;
		else if(token.equals("class")) 
			tipo =Token.CLASE;
		else {
			//Cadenas validas
			Pattern pat = Pattern.compile("^[a-zA-Z]+$");
			Matcher mat = pat.matcher(token);
			if(mat.find()) 
				tipo = Token.IDENTIFICADOR;
			else {
				impresion.add("Error en la linea "+renglon+" token "+token);
				bandera = false;
				return;
			}
		}
		tokens.insertar(new Token(token,tipo,renglon));
		impresion.add(new Token(token,tipo,renglon).toString());
	}
	//Analisis Semantico
	public Token analisisSemantico(NodoDoble<Token> nodo) {
		Token to;
		String cadena=null;
		//por si las moscas 
		if(nodo!=null) {
			to = nodo.dato;
			
			//Recorrido de la tabla de simbolos --Serrajero
			
			////Para saber si un identificador esta repetido --Serrajero
		}
		
		////Metodos para validar los tipos de datos --CuateAntraz
		
		//Metodos para validar los valores por medio de una cadena --CuateAntraz
	}
	public String separaDelimitadores(String linea){
		for (String string : Arrays.asList("(",")","{","}","=",";")) {
			if(string.equals("=")) {
				if(linea.indexOf(">=")>=0) {
					linea = linea.replace(">=", " >= ");
					break;
				}
				if(linea.indexOf("<=")>=0) {
					linea = linea.replace("<=", " <= ");
					break;
				}
				if(linea.indexOf("==")>=0)
				{
					linea = linea.replace("==", " == ");
					break;
				}
			}
			if(linea.contains(string)) 
				linea = linea.replace(string, " "+string+" ");
		}
		return linea;
	}
	public int cuenta (String token) {

		int conta=0;
		NodoDoble<Token> Aux=tokens.getInicio();
		while(Aux !=null){
			if(Aux.dato.getValor().equals(token))
				conta++;
			Aux=Aux.siguiente;
		}	
		return conta;
	}
	public ArrayList<String> getmistokens() {
		return impresion;
	}
	//Cuenta y Tipo de dato --CuateAntrax
	
}
