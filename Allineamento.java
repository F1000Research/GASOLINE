import java.util.Vector;

public class Allineamento implements Comparable<Allineamento>
{
	private Vector<Integer>[] nodi;
	private int complexSize;
	private double isc;
	
        public Allineamento(Vector<Integer>[] nodi, int complexSize, double isc)
	{
		this.nodi=nodi;
		this.complexSize=complexSize;
		this.isc=isc;
	}
        
	public Vector<Integer>[] getMapping()
	{
		return nodi;
	}
        
	public int getComplexSize()
	{
		return complexSize;
	}
        
	public double getIsc()
	{
		return isc;
	}
        
	public int compareTo(Allineamento other)
	{
		if(this.complexSize>other.getComplexSize())
			return 1;
		else if(this.complexSize<other.getComplexSize())
			return -1;
		else
		{
			if(this.isc>other.getIsc())
				return 1;
			else if(this.isc<other.getIsc())
				return -1;
			else
				return 0;
		}
	}
        
	public String toString()
	{
		return "COMPLEX_SIZE = "+complexSize+" ; ISC = "+isc;
	}
}