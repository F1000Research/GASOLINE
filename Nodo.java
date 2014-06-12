import java.util.HashMap;

public class Nodo implements Comparable<Nodo>
{
    
	private int id;
	private String label;
	private HashMap<Integer, Double> adiacs;
	private int grado;
    
	public Nodo(int id, String label)
	{
		this.id=id;
		this.label=label;
		adiacs=new HashMap<Integer,Double>();
		grado=0;
	}
    
	public int getId()
	{
		return id;
	}
    
	public String getLabel()
	{
		return label;
	}
    
	public void setId(int n)
	{
		id=n;
	}
    
	public void setLabel(String n)
	{
		label=n;
	}
    
	public HashMap<Integer,Double> getAdiacs()
	{
		return adiacs;
	}
    
	public int grado()
	{
		return grado;
	}
    
	public void incGrado()
	{
		grado++;
	}
    
	public int compareTo(Nodo other)
	{
		return this.label.compareTo(other.getLabel());
	}
    
	public String toString()
	{
		return label;
	}
}