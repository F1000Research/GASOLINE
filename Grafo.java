import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

//GRAFO NON-ORIENTATO
public class Grafo
{
	
	private HashMap<Integer,Nodo> nodi;
	private String name;
    
	public Grafo()
	{
		nodi=new HashMap<Integer,Nodo>();
	}
    
	public Grafo(String name)
	{
		nodi=new HashMap<Integer,Nodo>();
		this.name=name;
	}
    
	public String getName()
	{
		return name;
	}
    
	public boolean isEmpty()
	{
		return (numNodi()==0);
	}
    
	public int numNodi()
	{
		return nodi.size();
	}
    
	public HashMap<Integer,Nodo> nodi()
	{	
		return nodi;
	}
    
	public void addNode(int id, String label)
	{
		Nodo x=new Nodo(id, label);
		nodi.put(id,x);
	}
    
	public void addArc(int idSource, int idDest)
	{
		addArc(idSource, idDest, 0.0);
	}
    
	public void addArc(int idSource, int idDest, double weight)
	{
		Nodo source=nodi.get(idSource);
		Nodo dest=nodi.get(idDest);
		if(source!=null && dest!=null)
		{
			HashMap<Integer,Double> adiac=source.getAdiacs();
			adiac.put(idDest,weight);
			//Commentare le successive linee di codice per avere un grafo ORIENTATO
			adiac=dest.getAdiacs();
			adiac.put(idSource,weight);
			source.incGrado();
			dest.incGrado();
		}
		else
			System.out.println("Errore! Almeno uno dei due nodi non fa parte del grafo!");
	}
    
	public void removeNode(int id)
	{
		nodi.remove(id);
		HashMap<Integer,Double> adiac=null;
		Set<Integer> keys=nodi.keySet();
		Iterator<Integer> it=keys.iterator(); 
		while(it.hasNext())
		{
			int idSource=it.next();
			Nodo source=nodi.get(idSource);
			adiac=source.getAdiacs();
			if(adiac.containsKey(id))
				adiac.remove(id);
		}
	}
    
	public String toString()
	{
		if(isEmpty())
			return "Il grafo e' vuoto";
		else 
		{
			String str="Nodes = {";
			Set<Integer> keys=nodi.keySet();
			Iterator<Integer> it=keys.iterator();
			int id=it.next();
			str+=nodi.get(id).getLabel();
			while(it.hasNext())
			{
				id=it.next();
				str+=", "+nodi.get(id).getLabel();
			}
			str+="}\n";
			str+="Edges = { ";
			int i=0, j=0;
			Nodo source=null;
			HashMap<Integer,Double> adiac=null;
			keys=nodi.keySet();
			it=keys.iterator(); 
			while(it.hasNext())
			{
				int idSource=it.next();
				//System.out.print("\n"+idSource+": ");
				source=nodi.get(idSource);
				adiac=source.getAdiacs();
				Set<Integer> keys2=adiac.keySet();
				Iterator<Integer> it2=keys2.iterator();
				while(it2.hasNext())
				{
					int idDest=it2.next();
					//System.out.print(idDest+"\t");
					String s=source.getLabel();
					String d=nodi.get(idDest).getLabel();
					if(idSource<idDest)
						str+="("+s+","+d+","+adiac.get(idDest)+") ";
				}
			}
			str+="}\n";
			return str;
		}
	}
}