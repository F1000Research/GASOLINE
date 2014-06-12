public class Degree implements Comparable<Degree>
{
	private int idNodo;
	private int degree;
    
	public Degree(int idNodo, int degree)
	{
		this.degree=degree;
		this.idNodo=idNodo;
	}
    
	public double getDegree()
	{
		return degree;
	}
    
	public int getId()
	{
		return idNodo;
	}
    
	public int compareTo(Degree other)
	{
		if(degree<other.getDegree())
			return -1;
		else if (degree>other.getDegree())
			return 1;
		else
			return 0;
	}
}